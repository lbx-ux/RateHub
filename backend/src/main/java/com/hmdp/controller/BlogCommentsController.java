package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;
import com.hmdp.service.IBlogCommentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/blog-comments")
@RequiredArgsConstructor
public class BlogCommentsController {

    private final IBlogCommentsService blogCommentsService;

    @PostMapping
    public Result<?> saveComment(@RequestBody BlogComments blogComments) {
        return blogCommentsService.saveComment(blogComments);
    }

    @GetMapping
    public Result<?> queryCommentsByBlogId(@RequestParam("blogId") Long blogId) {
        return blogCommentsService.queryCommentsByBlogId(blogId);
    }
}
