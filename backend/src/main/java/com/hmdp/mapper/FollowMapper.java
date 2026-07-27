package com.hmdp.mapper;

import com.hmdp.entity.Follow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
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
public interface FollowMapper {

    @Select("SELECT count(*) FROM tb_follow WHERE user_id = #{userId} AND follow_user_id = #{followUserId}")
    int selectCount(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

    @Insert("INSERT INTO tb_follow (user_id, follow_user_id, create_time) VALUES (#{userId}, #{followUserId}, NOW())")
    void insert(Follow follow);

    @Delete("DELETE FROM tb_follow WHERE user_id = #{userId} AND follow_user_id = #{followUserId}")
    void delete(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

    @Select("SELECT follow_user_id FROM tb_follow WHERE user_id = #{userId}")
    List<Long> selectFollowUserIds(@Param("userId") Long userId);
}
