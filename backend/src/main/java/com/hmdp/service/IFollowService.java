package com.hmdp.service;

import com.hmdp.dto.Result;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService {

    Result<?> isFollow(Long followUserId);

    Result<?> follow(Long followUserId, Boolean isFollow);

    Result<?> followCommons(Long followUserId);
}
