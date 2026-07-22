package com.hmdp.mapper;

import com.hmdp.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface UserMapper {

    @Select("SELECT * FROM tb_user WHERE id = #{id}")
    User getById(Long id);

    @Select("select * from tb_user where phone=#{phone}")
    User getByPhone(String phone);

    @Insert("insert into tb_user(phone,nick_name)values (#{phone},#{nickName})")
    void save(User user);

    @org.apache.ibatis.annotations.Update("UPDATE tb_user SET nick_name = #{nickName}, icon = #{icon} WHERE id = #{id}")
    void updateProfile(User user);

    @org.apache.ibatis.annotations.Update("UPDATE tb_user SET password = #{password} WHERE id = #{id}")
    void updatePassword(User user);
}
