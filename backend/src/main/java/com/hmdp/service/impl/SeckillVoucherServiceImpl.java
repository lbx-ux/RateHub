package com.hmdp.service.impl;

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

    @Override
    public void save(SeckillVoucher seckillVoucher) {
        seckillVoucherMapper.insert(seckillVoucher);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
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

        // 4. 扣减库存
        int updateRows = seckillVoucherMapper.updateById(voucherId);
        if (updateRows < 1) {
            return Result.error("库存不足！");
        }
        log.info("还有{}库存", seckillVoucher.getStock());

        // 5. 获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.error("请先登录！");
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
