package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements IFollowService {

    private final FollowMapper followMapper;
    private final IUserService userService;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<?> queryMyFollows() {
        UserDTO user = UserHolder.getUser();
        List<Long> ids = followMapper.selectFollowUserIds(user.getId());
        return Result.success(users(ids));
    }

    @Override
    public Result<?> queryMyFans() {
        UserDTO user = UserHolder.getUser();
        List<Long> ids = followMapper.selectFanUserIds(user.getId());
        return Result.success(users(ids));
    }

    public List<UserDTO> users(List<Long> ids){
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserDTO> users = new ArrayList<>();
        for (Long id : ids) {
            User u = userService.getById(id);
            if (u != null) {
                users.add(BeanUtil.copyProperties(u, UserDTO.class));
            }
        }
        return users;
    }

    @Override
    public Result<?> isFollow(Long followUserId) {
        UserDTO user = UserHolder.getUser();
        int count = followMapper.selectCount(user.getId(), followUserId);
        return Result.success(count > 0);
    }

    @Override
    public Result<?> follow(Long followUserId, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        String key = "follow:" + userId;
        if (BooleanUtils.isTrue(isFollow)) {
            int count = followMapper.selectCount(userId, followUserId);
            if (count == 0) {
                Follow follow = new Follow();
                follow.setUserId(userId);
                follow.setFollowUserId(followUserId);
                boolean isSuccess = followMapper.insert(follow);
                if(isSuccess){
                    stringRedisTemplate.opsForSet().add(key, followUserId.toString());
                }
            }
        } else {
            boolean isSuccess=followMapper.delete(userId, followUserId);
            if(isSuccess){
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.success();
    }

    @Override
    public Result<?> followCommons(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        String key1="follow:"+userId;
        String key2="follow:"+followUserId;
        Set<String> commonFriends=stringRedisTemplate.opsForSet().intersect(key1,key2);
        if(commonFriends==null||commonFriends.isEmpty()){
            return Result.success(Collections.emptyList());
        }
        //将 Set<String> 转换为 List<Long>
        List<Long>ids=commonFriends.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> users=userMapper.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.success(users);
    }
}
