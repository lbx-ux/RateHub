package com.hmdp.mapper;

import com.hmdp.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

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

    @Select("SELECT * FROM tb_user")
    List<User> selectAll();


    List<User> listByIds(@Param("ids") List<Long> ids);
}
