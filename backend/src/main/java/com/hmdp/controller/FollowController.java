package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/follow")
@RequiredArgsConstructor
public class FollowController {

    private final IFollowService followService;

    //查询当前登录用户的“关注列表”
    @GetMapping("/of/me")
    public Result<?> queryMyFollows() {
        return followService.queryMyFollows();
    }

    //查询当前登录用户的“粉丝列表”
    @GetMapping("/fans/me")
    public Result<?> queryMyFans() {
        return followService.queryMyFans();
    }

    //查询当前登录用户是否已经关注指定目标达人
    @GetMapping("/or/not/{id}")
    public Result<?> isFollow(@PathVariable("id") Long followUserId) {
        return followService.isFollow(followUserId);
    }

    //关注或取消关注指定达人
    @PutMapping("/{id}/{isFollow}")
    public Result<?> follow(@PathVariable("id") Long followUserId, @PathVariable Boolean isFollow) {
        return followService.follow(followUserId, isFollow);
    }

    //查询当前登录用户与目标达人的“共同关注”列表
    @GetMapping("/common/{id}")
    public Result<?> followCommons(@PathVariable("id") Long followUserId) {
        return followService.followCommons(followUserId);
    }
}
