package com.hmdp.mapper;

import com.hmdp.entity.Blog;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface BlogMapper {

    @Select("SELECT * FROM tb_blog WHERE id = #{id}")
    Blog getById(Long id);

    @Insert("INSERT INTO tb_blog (shop_id, user_id, title, images, content, liked, comments, create_time, update_time) " +
            "VALUES (#{shopId}, #{userId}, #{title}, #{images}, #{content}, #{liked}, #{comments}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Blog blog);
    

    @Select("SELECT * FROM tb_blog WHERE user_id = #{userId}")
    List<Blog> queryMyBlog(@Param("userId") Long userId);

    @Select("SELECT * FROM tb_blog ORDER BY liked DESC")
    List<Blog> queryHotBlog();

    @Update("UPDATE tb_blog SET liked = liked + 1 WHERE id = #{id}")
    int addLiked(Long id);

    @Update("UPDATE tb_blog SET liked = IF(liked > 0, liked - 1, 0) WHERE id = #{id}")
    int subLiked(Long id);

    @Update("UPDATE tb_blog SET comments = IFNULL(comments, 0) + 1 WHERE id = #{id}")
    void addComments(Long id);

    @Delete("DELETE FROM tb_blog WHERE id = #{id}")
    int deleteById(Long id);

    List<Blog> selectByIds(List<Long> ids);
}
