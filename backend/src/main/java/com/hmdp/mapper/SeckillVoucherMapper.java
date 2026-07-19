package com.hmdp.mapper;

import com.hmdp.entity.SeckillVoucher;
import org.apache.ibatis.annotations.Insert;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
public interface SeckillVoucherMapper {

    @Insert("INSERT INTO tb_seckill_voucher (voucher_id, stock, begin_time, end_time, create_time, update_time) " +
            "VALUES (#{voucherId}, #{stock}, #{beginTime}, #{endTime}, #{createTime}, #{updateTime})")
    void insert(SeckillVoucher seckillVoucher);
}
