package com.hmdp.service;

import com.hmdp.entity.Blog;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService {

    void save(Blog blog);

    void updateLiked(Long id);

    List<Blog> queryMyBlog(Long userId, int current, int pageSize);

    List<Blog> queryHotBlog(int current, int pageSize);
}
