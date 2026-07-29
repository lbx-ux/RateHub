package com.hmdp.mapper;

import com.hmdp.entity.BlogComments;

import org.apache.ibatis.annotations.Delete;
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
public interface BlogCommentsMapper {

    @Insert("INSERT INTO tb_blog_comments (user_id, blog_id, parent_id, answer_id, content, liked, status, create_time, update_time) " +
            "VALUES (#{userId}, #{blogId}, #{parentId}, #{answerId}, #{content}, #{liked}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(BlogComments blogComments);

    @Select("SELECT * FROM tb_blog_comments WHERE blog_id = #{blogId} AND status = 0 ORDER BY create_time DESC")
    List<BlogComments> queryByBlogId(@Param("blogId") Long blogId);

    @Delete("DELETE FROM tb_blog_comments WHERE blog_id = #{blogId}")
    void deleteByBlogId(@Param("blogId") Long blogId);
}
