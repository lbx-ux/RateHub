package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.BlogComments;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
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
public class BlogCommentsServiceImpl implements IBlogCommentsService {

    @Resource
    private BlogCommentsMapper blogCommentsMapper;

    @Resource
    private BlogMapper blogMapper;

    @Resource
    private IUserService userService;

    @Override
    @Transactional
    public Result<?> saveComment(BlogComments blogComments) {
        if (blogComments.getContent() == null || blogComments.getContent().trim().isEmpty()) {
            return Result.error("评论内容不能为空！");
        }
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.error("请先登录！");
        }
        blogComments.setUserId(user.getId());
        if (blogComments.getParentId() == null) {
            blogComments.setParentId(0L);
        }
        if (blogComments.getAnswerId() == null) {
            blogComments.setAnswerId(0L);
        }
        blogComments.setLiked(0);
        blogComments.setStatus(false);
        blogComments.setCreateTime(LocalDateTime.now());
        blogComments.setUpdateTime(LocalDateTime.now());

        blogCommentsMapper.insert(blogComments);
        blogMapper.addComments(blogComments.getBlogId());

        return Result.success();
    }

    @Override
    public Result<?> queryCommentsByBlogId(Long blogId) {
        if (blogId == null) {
            return Result.error("博客ID不能为空！");
        }
        List<BlogComments> list = blogCommentsMapper.queryByBlogId(blogId);
        if (list != null && !list.isEmpty()) {
            list.forEach(comment -> {
                User user = userService.getById(comment.getUserId());
                if (user != null) {
                    comment.setNickName(user.getNickName());
                    comment.setIcon(user.getIcon());
                } else {
                    comment.setNickName("已注销用户");
                    comment.setIcon("");
                }
            });
        }
        return Result.success(list);
    }
}
