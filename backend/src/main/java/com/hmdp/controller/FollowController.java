package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
@RequiredArgsConstructor
public class FollowController {

    private final IFollowService followService;

    @GetMapping("/or/not/{id}")
    public Result<?> isFollow(@PathVariable("id") Long followUserId) {
        return followService.isFollow(followUserId);
    }

    @PutMapping("/{id}/{isFollow}")
    public Result<?> follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(followUserId, isFollow);
    }

    @GetMapping("/common/{id}")
    public Result<?> followCommons(@PathVariable("id") Long followUserId) {
        return followService.followCommons(followUserId);
    }
}
