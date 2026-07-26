package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeckillVoucherServiceImpl implements ISeckillVoucherService {
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final RedisIdWorker redisIdWorker;
    private final VoucherOrderMapper voucherOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    @Override
    public void save(SeckillVoucher seckillVoucher) {
        seckillVoucherMapper.insert(seckillVoucher);
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

        // 3. 判断库存
        if (seckillVoucher.getStock() < 1) {
            return Result.error("库存不足！");
        }

        // ... 前置的库存判断逻辑...

        Long userId = UserHolder.getUser().getId();

        // 1. 获取锁对象 (直接复用 Redisson 客户端，不再手动传入 StringRedisTemplate)
        RLock lock = redissonClient.getLock("lock:order:" + userId);

         // 2. 尝试获取分布式锁
        // 亮点：无参 tryLock() 默认开启 WatchDog (看门狗) 机制
        // 只要业务没执行完，后台会自动每 10s 续期到 30s，彻底告别锁提前超时释放的风险
        boolean isLock = lock.tryLock();

        // 3. 拦截重复请求
        if (!isLock) {
            // 获取锁失败，说明当前用户已有并发请求正在处理中
            // 直接返回，完美实现“一人一单”防重提交
            return Result.error("不允许重复下单！");
        }

        try {
            // 4. 获取当前类的 AOP 代理对象
            // 亮点：避免 this 调用导致的 Spring 声明式事务 (@Transactional) 失效
            ISeckillVoucherService proxy = (ISeckillVoucherService) AopContext.currentProxy();

            // 5. 调用代理对象的下单方法，执行真正的核心业务逻辑 (查单、扣库存、创单)
            return proxy.createVoucherOrder(voucherId);

        } finally {
            // 6. 安全释放锁 (防御性编程的核心)
            // 亮点：释放前必须查验归属权，防止极端情况下误删别人的锁，或抛出 IllegalMonitorStateException 异常
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }

    @Transactional(rollbackFor = Exception.class)
    public Result<?> createVoucherOrder(Long voucherId) {
        // 5. 获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.error("请先登录！");
        }
        int count = voucherOrderMapper.queryCountByVoucherId(voucherId, user.getId());
        if (count > 0) {
            return Result.error("该用户已经购买过该优惠券！");
        }

        // 4. 扣减库存
        int updateRows = seckillVoucherMapper.updateById(voucherId);
        if (updateRows < 1) {
            return Result.error("库存不足！");
        }

        // 6. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(user.getId());
        voucherOrder.setVoucherId(voucherId);

        voucherOrderMapper.save(voucherOrder);

        // 7. 返回订单 ID
        return Result.success(orderId);
    }
}
