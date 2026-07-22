package com.hmdp.mapper;

import com.hmdp.entity.Voucher;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface VoucherMapper {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);

    @Insert("INSERT INTO tb_voucher (shop_id, title, sub_title, rules, pay_value, actual_value, type, status, create_time, update_time) " +
            "VALUES (#{shopId}, #{title}, #{subTitle}, #{rules}, #{payValue}, #{actualValue}, #{type}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Voucher voucher);

    @Select("SELECT id, shop_id, title, sub_title, rules, pay_value, actual_value, type, status, create_time, update_time FROM tb_voucher WHERE id = #{id}")
    Voucher getById(@Param("id") Long id);
}
