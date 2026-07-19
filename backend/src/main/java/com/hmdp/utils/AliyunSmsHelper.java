package com.hmdp.utils;

import cn.hutool.json.JSONUtil;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.hmdp.config.AliyunSmsProperties;
import com.hmdp.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class AliyunSmsHelper {

    private final AliyunSmsProperties aliyunSmsProperties;

    private Client client;

    @PostConstruct
    public void init() throws Exception {
        if (aliyunSmsProperties == null 
                || !StringUtils.hasText(aliyunSmsProperties.getAccessKeyId()) 
                || !StringUtils.hasText(aliyunSmsProperties.getAccessKeySecret())) {
            throw new IllegalStateException("Aliyun SMS AccessKey properties are not configured properly!");
        }
        Config config = new Config()
                .setAccessKeyId(aliyunSmsProperties.getAccessKeyId())
                .setAccessKeySecret(aliyunSmsProperties.getAccessKeySecret());
        config.endpoint = "dypnsapi.aliyuncs.com";
        this.client = new Client(config);
    }

    /**
     * 发送短信验证码
     * @param phone 目标手机号
     */
    public void sendSmsCode(String phone) {
        try {
            // 使用 Map 构造并使用 JSONUtil 转为 JSON 串，避免不规范的硬编码转义字符
            Map<String, String> paramMap = new HashMap<>(2);
            paramMap.put("code", "##code##");
            paramMap.put("min", "5");
            String templateParam = JSONUtil.toJsonStr(paramMap);

            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setSignName(aliyunSmsProperties.getSignName())
                    .setTemplateCode(aliyunSmsProperties.getTemplateCode())
                    .setTemplateParam(templateParam)
                    .setCodeLength(6L)
                    .setValidTime(300L);

            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCode(request);

            if ("OK".equals(response.getBody().getCode())) {
                log.info("短信验证码发送成功，目标手机号：{}", phone);
            } else {
                log.error("短信发送失败响应：Code = {}, Message = {}", 
                        response.getBody().getCode(), response.getBody().getMessage());
                throw new BusinessException("验证码发送失败！！！");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用短信发送接口遇到未知异常", e);
            throw new BusinessException("程序出错，请过一段时间再试！");
        }
    }

    /**
     * 校验短信验证码是否正确
     * @param phone 手机号
     * @param verifyCode 用户输入的验证码
     * @return 是否通过
     */
    public boolean verifySmsCode(String phone, String verifyCode) {
        try {
            CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setVerifyCode(verifyCode);

            CheckSmsVerifyCodeResponse response = client.checkSmsVerifyCode(request);

            if ("OK".equals(response.getBody().getCode())
                    && response.getBody().getModel() != null
                    && "PASS".equals(response.getBody().getModel().getVerifyResult())) {
                log.info("验证码核验通过！手机号：{}", phone);
                return true;
            } else {
                log.warn("验证码核验失败响应：Code = {}, Message = {}", 
                        response.getBody().getCode(), response.getBody().getMessage());
                return false;
            }
        } catch (TeaException e) {
            log.error("调用核验接口失败 - 阿里云异常信息: Code = {}, Message = {}", e.getCode(), e.getMessage());
            // 阿里云核验验证码不匹配时抛出的是 code: 400，message: 验证失败。此处应妥善作为校验失败返回
            if ("400".equals(e.getCode()) || (e.getMessage() != null && e.getMessage().contains("验证失败"))) {
                return false;
            }
            throw new BusinessException("程序出错，请过一段时间再试！");
        } catch (Exception e) {
            log.error("调用核验接口遇到未知网络/系统异常", e);
            throw new BusinessException("程序出错，请过一段时间再试！");
        }
    }
}
