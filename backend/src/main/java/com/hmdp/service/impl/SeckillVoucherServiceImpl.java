package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeckillVoucherServiceImpl implements ISeckillVoucherService {
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final RedisIdWorker redisIdWorker;
    private final VoucherOrderMapper voucherOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    @Autowired
    @Lazy
    private ISeckillVoucherService selfService;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // 1. 从消费组获取 stream.orders 队列中的最新消息
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create("stream.orders", ReadOffset.lastConsumed())
                    );

                    if (list == null || list.isEmpty()) {
                        continue;
                    }

                    // 2. 解析消息并转为 VoucherOrder 实体
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);

                    // 3. 执行 MySQL 落库
                    handleVoucherOrder(voucherOrder);

                    // 4. 手动 ACK 确认消息
                    stringRedisTemplate.opsForStream().acknowledge("stream.orders", "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常，转向 PendingList", e);
                    handlePendingList();
                }
            }
        }
    }

    private void handlePendingList() {
        while (true) {
            try {
                // 读取 ReadOffset.from("0") 即 Pending List 中的历史异常消息
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create("stream.orders", ReadOffset.from("0"))
                );

                if (list == null || list.isEmpty()) {
                    break;
                }

                MapRecord<String, Object, Object> record = list.get(0);
                Map<Object, Object> value = record.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);

                handleVoucherOrder(voucherOrder);

                stringRedisTemplate.opsForStream().acknowledge("stream.orders", "g1", record.getId());
            } catch (Exception e) {
                log.error("PendingList 处理异常，休眠后重试", e);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            log.error("不允许重复下单！");
            return;
        }
        try {
            selfService.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Result<?> seckillVoucher(Long voucherId) {
        // 1. 查询秒杀券
        SeckillVoucher seckillVoucher = seckillVoucherMapper.getById(voucherId);
        if (seckillVoucher == null) {
            return Result.error("秒杀优惠券不存在！");
        }
        // 2. 判断秒杀时间
        LocalDateTime now = LocalDateTime.now();
        if (seckillVoucher.getBeginTime().isAfter(now)) {
            return Result.error("秒杀尚未开始！");
        }
        if (seckillVoucher.getEndTime().isBefore(now)) {
            return Result.error("秒杀已经结束！");
        }

        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.error("请先登录！");
        }
        Long userId = user.getId();
        Long orderId = redisIdWorker.nextId("order");

        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                userId.toString(),
                voucherId.toString(),
                String.valueOf(orderId)
        );
        int r = result.intValue();
        if (r != 0) {
            return Result.error(r == 1 ? "库存不足!" : "不能重复下单!");
        }
        return Result.success(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();

        // 1. 一人一单 MySQL 层面兜底校验
        int count = voucherOrderMapper.queryCountByVoucherId(voucherId, userId);
        if (count > 0) {
            log.error("该用户已经购买过该优惠券！");
            return;
        }

        // 2. 扣减 MySQL 库存
        int updateRows = seckillVoucherMapper.updateById(voucherId);
        if (updateRows < 1) {
            log.error("MySQL 库存不足！");
            return;
        }

        // 3. 保存订单
        voucherOrderMapper.save(voucherOrder);
    }
}
