package com.hmdp.mapper;

import com.hmdp.entity.SeckillVoucher;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SeckillVoucherMapper {

    @Insert("INSERT INTO tb_seckill_voucher (voucher_id, stock, begin_time, end_time, create_time, update_time) " +
            "VALUES (#{voucherId}, #{stock}, #{beginTime}, #{endTime}, #{createTime}, #{updateTime})")
    void insert(SeckillVoucher seckillVoucher);

    @Select("select * from tb_seckill_voucher where voucher_id=#{voucherId}")
    SeckillVoucher getById(Long voucherId);

    @Update("update tb_seckill_voucher set stock=stock-1 where voucher_id=#{voucherId} and stock>0")
    int updateById(Long voucherId);
}
