package com.hmdp.service.impl;

import com.github.pagehelper.PageHelper;
import com.hmdp.entity.Blog;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl implements IBlogService {

    @Resource
    private BlogMapper blogMapper;

    @Override
    public void save(Blog blog) {
        blogMapper.insert(blog);
    }

    @Override
    public void updateLiked(Long id) {
        blogMapper.updateLiked(id);
    }

    @Override
    public List<Blog> queryMyBlog(Long userId, int current, int pageSize) {
        PageHelper.startPage(current, pageSize);
        return blogMapper.queryMyBlog(userId);
    }

    @Override
    public List<Blog> queryHotBlog(int current, int pageSize) {
        PageHelper.startPage(current, pageSize);
        return blogMapper.queryHotBlog();
    }
}
