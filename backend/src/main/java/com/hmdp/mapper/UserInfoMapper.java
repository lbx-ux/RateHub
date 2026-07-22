package com.hmdp.mapper;

import com.hmdp.entity.UserInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
public interface UserInfoMapper {

    @Select("SELECT * FROM tb_user_info WHERE user_id = #{userId}")
    UserInfo getById(@Param("userId") Long userId);

    @org.apache.ibatis.annotations.Update("INSERT INTO tb_user_info (user_id, city, introduce, gender, birthday) VALUES (#{userId}, #{city}, #{introduce}, #{gender}, #{birthday}) ON DUPLICATE KEY UPDATE city=VALUES(city), introduce=VALUES(introduce), gender=VALUES(gender), birthday=VALUES(birthday)")
    void insertOrUpdate(UserInfo userInfo);
}
