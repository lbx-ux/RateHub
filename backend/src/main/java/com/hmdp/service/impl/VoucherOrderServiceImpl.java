package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherOrderServiceImpl implements IVoucherOrderService {
    private final VoucherOrderMapper voucherOrderMapper;
    private final VoucherMapper voucherMapper;
    private final RedisIdWorker redisIdWorker;

    @Override
    public Result buyNormalVoucher(Long voucherId) {
        // 1. 查询普通代金券
        Voucher voucher = voucherMapper.getById(voucherId);
        if (voucher == null) {
            return Result.error("优惠券不存在！");
        }

        // 2. 校验是否为普通代金券 (type = 0)
        if (voucher.getType() != null && voucher.getType() != 0) {
            return Result.error("该优惠券非普通代金券，请前往对应入口购买！");
        }

        // 3. 获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.error("请先登录！");
        }

        // 4. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(user.getId());
        voucherOrder.setVoucherId(voucherId);

        // 5. 保存订单
        voucherOrderMapper.save(voucherOrder);

        // 6. 返回订单 ID
        return Result.success(orderId);
    }
}
