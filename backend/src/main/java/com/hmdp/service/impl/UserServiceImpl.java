package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.constant.RedisConstants;
import com.hmdp.dto.CaptchaCodeDTO;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.exception.BusinessException;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.AliyunCaptchaHelper;
import com.hmdp.utils.AliyunSmsHelper;
import com.hmdp.utils.RegexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserMapper userMapper;
    private final com.hmdp.mapper.UserInfoMapper userInfoMapper;
    private final AliyunSmsHelper aliyunSmsHelper;
    private final AliyunCaptchaHelper aliyunCaptchaHelper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public User getById(Long id) {
        return userMapper.getById(id);
    }

    @Override
    public Result<String> sendCode(CaptchaCodeDTO dto, HttpSession session) {
        String phone = dto.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new BusinessException("手机号格式错误！！！");
        }

        // 1. 调用阿里云图形验证码二次校验
        boolean captchaPass = aliyunCaptchaHelper.verifyCaptcha(
                dto.getLotNumber(),
                dto.getCaptchaOutput(),
                dto.getPassToken(),
                dto.getGenTime()
        );
        if (!captchaPass) {
            throw new BusinessException("人机验证未通过，请重新尝试");
        }

        // 2. 验证通过后发送短信验证码
        aliyunSmsHelper.sendSmsCode(phone);
        return Result.success();
    }

    @Override
    public Result<String> login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String userInputCode = loginForm.getCode();
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new BusinessException("手机号格式错误！！！");
        }

        User user = userMapper.getByPhone(phone);

        if (StrUtil.isNotBlank(loginForm.getPassword())) {
            // 密码登录
            if (user == null) {
                throw new BusinessException("该手机号尚未注册，请先使用验证码登录");
            }
            String md5Hex = cn.hutool.crypto.SecureUtil.md5(loginForm.getPassword());
            if (!md5Hex.equals(user.getPassword())) {
                throw new BusinessException("密码错误");
            }
        } else {
            // 验证码登录
            boolean verifyPass = aliyunSmsHelper.verifySmsCode(phone, userInputCode);
            if (!verifyPass) {
                throw new BusinessException("验证码核验失败！！！");
            }
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                user.setNickName("user_" + RandomUtil.randomNumbers(10));
                user.setIcon("");
                userMapper.save(user);
            }
        }

        // 1. 生成新 token 之前，先检查该用户是否已有登录的 token，实现“踢人下线”
        String userTokenKey = "login:user:" + user.getId();
        String oldToken = stringRedisTemplate.opsForValue().get(userTokenKey);
        if (StrUtil.isNotBlank(oldToken)) {
            // 如果存在旧 token，则将其删除
            stringRedisTemplate.delete(RedisConstants.LOGIN_USER_KEY + oldToken);
        }

        // 2. 随机生成 token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        log.info("token:{}", token);

        // 3. 将 User 转换为 UserDTO
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

        // 4. 将 UserDTO 对象转为 Map
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? "" : fieldValue.toString()));
        
        // 5. 以 Hash 结构存储用户数据到 Redis
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 设置 token 的有效期（30分钟）
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);

        // 6. 记录 当前用户ID -> 最新Token 的映射，并保持同样的有效期
        stringRedisTemplate.opsForValue().set(userTokenKey, token, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);

        // 7. 返回 token 给前端
        return Result.success(token);
    }

    @Override
    public Result updateProfile(com.hmdp.dto.UserUpdateDTO dto, String token) {
        UserDTO userDTO = com.hmdp.utils.UserHolder.getUser();
        if (userDTO == null) {
            return Result.error("尚未登录！");
        }

        // 1. Update tb_user
        User user = new User();
        user.setId(userDTO.getId());
        user.setNickName(dto.getNickName());
        user.setIcon(dto.getIcon());
        userMapper.updateProfile(user);

        // 2. Update tb_user_info
        com.hmdp.entity.UserInfo userInfo = new com.hmdp.entity.UserInfo();
        userInfo.setUserId(userDTO.getId());
        
        Boolean gender = null;
        if ("男".equals(dto.getGender())) gender = false;
        else if ("女".equals(dto.getGender())) gender = true;
        userInfo.setGender(gender);
        
        if (cn.hutool.core.util.StrUtil.isNotBlank(dto.getBirthday())) {
            userInfo.setBirthday(java.time.LocalDate.parse(dto.getBirthday()));
        }
        
        userInfo.setCity(dto.getCity());
        userInfo.setIntroduce(dto.getIntroduce());
        userInfoMapper.insertOrUpdate(userInfo);

        // 3. Update Redis cache
        if (cn.hutool.core.util.StrUtil.isNotBlank(token)) {
            String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
            if (cn.hutool.core.util.StrUtil.isNotBlank(dto.getNickName())) {
                stringRedisTemplate.opsForHash().put(tokenKey, "nickName", dto.getNickName());
            }
            if (cn.hutool.core.util.StrUtil.isNotBlank(dto.getIcon())) {
                stringRedisTemplate.opsForHash().put(tokenKey, "icon", dto.getIcon());
            }
        }

        return Result.success();
    }

    @Override
    public Result updatePassword(String oldPassword, String newPassword) {
        UserDTO userDTO = com.hmdp.utils.UserHolder.getUser();
        if (userDTO == null) {
            return Result.error("尚未登录！");
        }
        User user = userMapper.getById(userDTO.getId());
        if (user == null) {
            return Result.error("用户不存在！");
        }

        // Check old password
        if (cn.hutool.core.util.StrUtil.isNotBlank(user.getPassword())) {
            if (cn.hutool.core.util.StrUtil.isBlank(oldPassword)) {
                return Result.error("请输入原密码");
            }
            String oldMd5 = cn.hutool.crypto.SecureUtil.md5(oldPassword);
            if (!oldMd5.equals(user.getPassword())) {
                return Result.error("原密码错误");
            }
        }

        // Set new password
        String newMd5 = cn.hutool.crypto.SecureUtil.md5(newPassword);
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPassword(newMd5);
        userMapper.updatePassword(updateUser);
        return Result.success();
    }
}
