package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;

import java.util.List;

public interface IBlogService {
    

    List<Blog> queryMyBlog(Long userId, int current, int pageSize);

    List<Blog> queryHotBlog(int current, int pageSize);

    Result<?> save(Blog blog);

    Result<?> queryBlogById(Long id);

    void updateLiked(Long id);

    Result<?> queryBlogLikes(Long id);

    Result<?> deleteBlog(Long id);
}
