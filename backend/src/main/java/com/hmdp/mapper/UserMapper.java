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
}
