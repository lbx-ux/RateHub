package com.hmdp.mapper;

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

    @Select("SELECT * FROM tb_shop WHERE type_id = #{typeId}")
    List<Shop> queryShopByType(@Param("typeId") Integer typeId);

    @Select("<script>" +
            "SELECT * FROM tb_shop " +
            "<where>" +
            "  <if test='name != null and name != \"\"'>" +
            "    name LIKE CONCAT('%', #{name}, '%')" +
            "  </if>" +
            "</where>" +
            "</script>")
    List<Shop> queryShopByName(@Param("name") String name);
}
