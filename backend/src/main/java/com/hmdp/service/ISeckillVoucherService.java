package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;

public interface ISeckillVoucherService {

    void save(SeckillVoucher seckillVoucher);

    Result seckillVoucher(Long voucherId);
}
