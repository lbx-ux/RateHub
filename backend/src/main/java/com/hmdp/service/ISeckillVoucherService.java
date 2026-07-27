package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;

public interface ISeckillVoucherService {

    Result<?> seckillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);
}
