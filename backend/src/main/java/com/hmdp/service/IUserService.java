package com.hmdp.service;

import com.hmdp.dto.CaptchaCodeDTO;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

public interface IUserService {

    User getById(Long id);

    Result<String> sendCode(CaptchaCodeDTO captchaCodeDTO, HttpSession session);

    Result<String> login(LoginFormDTO loginForm, HttpSession session);
}
