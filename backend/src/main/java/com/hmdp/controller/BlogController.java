package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.service.IBlogService;
import com.hmdp.constant.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/blog")
@RequiredArgsConstructor
public class BlogController {

    private final IBlogService blogService;

    @PostMapping
    public Result<?> saveBlog(@RequestBody Blog blog) {
        return blogService.save(blog);
    }

    @PutMapping("/like/{id}")
    public Result<?> likeBlog(@PathVariable("id") Long id) {
        // 修改点赞数
        blogService.updateLiked(id);
        return Result.success();
    }

    /**
     * 查询探店日记点赞排行榜（最早点赞的 Top 5 用户）
     * @param id 博文 ID
     * @return Top 5 点赞用户列表
     */
    @GetMapping("/likes/{id}")
    public Result<?> queryBlogLikes(@PathVariable("id") Long id) {
        return blogService.queryBlogLikes(id);
    }

    @GetMapping("/{id}")
    public Result<?> queryBlogById(@PathVariable("id") Long id) {
        return blogService.queryBlogById(id);
    }


    @GetMapping("/of/me")
    public Result<?> queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        List<Blog> records = blogService.queryMyBlog(user.getId(), current, SystemConstants.MAX_PAGE_SIZE);
        return Result.success(records);
    }

    @GetMapping("/hot")
    public Result<?> queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 根据热门博文分页查询
        List<Blog> records = blogService.queryHotBlog(current, SystemConstants.MAX_PAGE_SIZE);
        return Result.success(records);
    }

    @GetMapping("/of/user")
    public Result<?> queryBlogByUserId(
            @RequestParam("id") Long id,
            @RequestParam(value = "current", defaultValue = "1") Integer current) {
        List<Blog> records = blogService.queryMyBlog(id, current, SystemConstants.MAX_PAGE_SIZE);
        return Result.success(records);
    }
}
