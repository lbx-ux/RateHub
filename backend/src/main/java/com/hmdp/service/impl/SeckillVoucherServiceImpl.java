package com.hmdp.service.impl;

import com.hmdp.entity.SeckillVoucher;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
@Service
public class SeckillVoucherServiceImpl implements ISeckillVoucherService {

    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;

    @Override
    public void save(SeckillVoucher seckillVoucher) {
        seckillVoucherMapper.insert(seckillVoucher);
    }
}
