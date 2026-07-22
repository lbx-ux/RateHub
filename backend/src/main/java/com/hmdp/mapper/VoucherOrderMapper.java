package com.hmdp.mapper;

import com.hmdp.entity.VoucherOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface VoucherOrderMapper {

    @Insert("insert into tb_voucher_order (id, user_id, voucher_id) values (#{id}, #{userId}, #{voucherId})")
    void save(VoucherOrder voucherOrder);

    @Select("select count(*) from tb_voucher_order where voucher_id=#{voucherId} and user_id=#{userId}")
    int queryCountByVoucherId(Long voucherId, Long userId);

    @Select("select o.id, o.voucher_id as voucherId, v.shop_id as shopId, v.title, v.pay_value as payValue, v.actual_value as actualValue, v.type, o.status, o.create_time as createTime " +
            "from tb_voucher_order o " +
            "left join tb_voucher v on o.voucher_id = v.id " +
            "where o.user_id = #{userId} " +
            "order by o.create_time desc")
    List<com.hmdp.dto.VoucherOrderVO> queryMyOrders(Long userId);
}
    
