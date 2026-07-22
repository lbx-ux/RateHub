package com.hmdp.mapper;

import com.hmdp.entity.VoucherOrder;
import org.apache.ibatis.annotations.Insert;

public interface VoucherOrderMapper {

    @Insert("insert into tb_voucher_order (id, user_id, voucher_id) values (#{id}, #{userId}, #{voucherId})")
    void save(VoucherOrder voucherOrder);
}
