package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
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
public class FollowServiceImpl implements IFollowService {

    @Resource
    private FollowMapper followMapper;

    @Resource
    private IUserService userService;

    @Override
    public Result<?> isFollow(Long followUserId) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.success(false);
        }
        int count = followMapper.selectCount(user.getId(), followUserId);
        return Result.success(count > 0);
    }

    @Override
    public Result<?> follow(Long followUserId, Boolean isFollow) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.error("请先登录");
        }
        Long userId = user.getId();
        if (Boolean.TRUE.equals(isFollow)) {
            int count = followMapper.selectCount(userId, followUserId);
            if (count == 0) {
                Follow follow = new Follow();
                follow.setUserId(userId);
                follow.setFollowUserId(followUserId);
                followMapper.insert(follow);
            }
        } else {
            followMapper.delete(userId, followUserId);
        }
        return Result.success();
    }

    @Override
    public Result<?> followCommons(Long followUserId) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.success(Collections.emptyList());
        }
        Long userId = user.getId();
        List<Long> myFollows = followMapper.selectFollowUserIds(userId);
        List<Long> targetFollows = followMapper.selectFollowUserIds(followUserId);
        if (myFollows == null || myFollows.isEmpty() || targetFollows == null || targetFollows.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        myFollows.retainAll(targetFollows);
        if (myFollows.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        List<UserDTO> users = new ArrayList<>();
        for (Long id : myFollows) {
            User u = userService.getById(id);
            if (u != null) {
                users.add(BeanUtil.copyProperties(u, UserDTO.class));
            }
        }
        return Result.success(users);
    }
}
