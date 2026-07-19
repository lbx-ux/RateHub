package com.hmdp.mapper;

import com.hmdp.entity.Blog;
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
public interface BlogMapper {

    @Select("SELECT * FROM tb_blog WHERE id = #{id}")
    Blog getById(Long id);

    @Insert("INSERT INTO tb_blog (shop_id, user_id, title, images, content, liked, comments, create_time, update_time) " +
            "VALUES (#{shopId}, #{userId}, #{title}, #{images}, #{content}, #{liked}, #{comments}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Blog blog);

    @Update("UPDATE tb_blog SET liked = liked + 1 WHERE id = #{id}")
    void updateLiked(Long id);

    @Select("SELECT * FROM tb_blog WHERE user_id = #{userId}")
    List<Blog> queryMyBlog(@Param("userId") Long userId);

    @Select("SELECT * FROM tb_blog ORDER BY liked DESC")
    List<Blog> queryHotBlog();
}
