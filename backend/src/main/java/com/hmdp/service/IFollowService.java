package com.hmdp.service;

import com.hmdp.dto.Result;

public interface IFollowService {

    Result<?> queryMyFollows();

    Result<?> queryMyFans();

    Result<?> isFollow(Long followUserId);

    Result<?> follow(Long followUserId, Boolean isFollow);

    Result<?> followCommons(Long followUserId);
}
