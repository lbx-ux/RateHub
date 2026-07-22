package com.hmdp.mapper;

import com.hmdp.dto.ShopVO;
import com.hmdp.entity.Shop;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface ShopMapper {

    @Select("SELECT * FROM tb_shop WHERE id = #{id}")
    Shop getById(Long id);

    @Insert("INSERT INTO tb_shop (name, type_id, images, area, address, x, y, avg_price, sold, comments, score, open_hours, create_time, update_time) " +
            "VALUES (#{name}, #{typeId}, #{images}, #{area}, #{address}, #{x}, #{y}, #{avgPrice}, #{sold}, #{comments}, #{score}, #{openHours}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Shop shop);

    @Update("UPDATE tb_shop SET name = #{name}, type_id = #{typeId}, images = #{images}, area = #{area}, address = #{address}, " +
            "x = #{x}, y = #{y}, avg_price = #{avgPrice}, sold = #{sold}, comments = #{comments}, score = #{score}, open_hours = #{openHours}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    void update(Shop shop);

    @Select("SELECT s.*, " +
            "  (EXISTS (SELECT 1 FROM tb_voucher v WHERE v.shop_id = s.id)) AS has_voucher " +
            "FROM tb_shop s WHERE s.type_id = #{typeId}")
    List<ShopVO> queryShopByType(@Param("typeId") Integer typeId);

    @Select("<script>" +
            "SELECT s.*, " +
            "  (EXISTS (SELECT 1 FROM tb_voucher v WHERE v.shop_id = s.id)) AS has_voucher " +
            "FROM tb_shop s " +
            "<where>" +
            "  <if test='name != null and name != \"\"'>" +
            "    s.name LIKE CONCAT('%', #{name}, '%')" +
            "  </if>" +
            "</where>" +
            "</script>")
    List<ShopVO> queryShopByName(@Param("name") String name);
}
