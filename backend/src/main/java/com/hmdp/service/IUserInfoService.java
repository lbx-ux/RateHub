package com.hmdp.service;

import com.hmdp.entity.UserInfo;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
public interface IUserInfoService {

    UserInfo getById(Long userId);
}
