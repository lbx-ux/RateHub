package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    private final ISeckillVoucherService seckillVoucherService;
    private final IVoucherOrderService voucherOrderService;

    /**
     * 秒杀优惠券下单接口
     */
    @PostMapping("/seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return seckillVoucherService.seckillVoucher(voucherId);
    }

    /**
     * 普通优惠券下单接口
     */
    @PostMapping("/normal/{id}")
    public Result buyNormalVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.buyNormalVoucher(voucherId);
    }
}
