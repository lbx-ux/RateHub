package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogCommentsService {

    Result<?> saveComment(BlogComments blogComments);

    Result<?> queryCommentsByBlogId(Long blogId);
}
