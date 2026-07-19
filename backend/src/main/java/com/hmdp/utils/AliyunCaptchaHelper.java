package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.config.AliyunCaptchaProperties;
import com.hmdp.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class AliyunCaptchaHelper {

    private final AliyunCaptchaProperties captchaProperties;

    /**
     * 向阿里云图形认证服务发起二次校验
     * @param lotNumber     前端 getValidate().lot_number
     * @param captchaOutput 前端 getValidate().captcha_output
     * @param passToken     前端 getValidate().pass_token
     * @param genTime       前端 getValidate().gen_time
     * @return true: 人机验证通过, false: 验证失败
     */
    public boolean verifyCaptcha(String lotNumber, String captchaOutput,
                                  String passToken, String genTime) {
        // 1. 计算 sign_token = HMAC-SHA256(lot_number, appKey) → hex 小写
        String signToken = SecureUtil.hmacSha256(captchaProperties.getAppKey())
                .digestHex(lotNumber);

        // 2. 构造 form 参数
        Map<String, Object> params = new HashMap<>();
        params.put("lot_number", lotNumber);
        params.put("captcha_output", captchaOutput);
        params.put("pass_token", passToken);
        params.put("gen_time", genTime);
        params.put("sign_token", signToken);

        // 3. 发送 POST 请求到二次校验接口
        String url = "https://captcha.alicaptcha.com/validate?captcha_id="
                + captchaProperties.getAppId();

        try {
            String responseBody = HttpUtil.createPost(url)
                    .contentType(ContentType.FORM_URLENCODED.getValue())
                    .form(params)
                    .timeout(5000)
                    .execute()
                    .body();

            // 4. 解析 JSON 响应
            JSONObject json = JSONUtil.parseObj(responseBody);
            String status = json.getStr("status");
            String result = json.getStr("result");

            if (StrUtil.equals(status, "error")) {
                log.error("图形验证码二次校验请求错误: code={}, msg={}",
                        json.getStr("code"), json.getStr("msg"));
                throw new BusinessException("图形验证服务异常，请稍后重试");
            }

            if (StrUtil.equals(result, "success")) {
                log.info("图形验证码二次校验通过");
                return true;
            }

            log.warn("图形验证码二次校验不通过: result={}, reason={}", result, json.getStr("reason"));
            return false;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用图形验证码二次校验接口遇到未知异常", e);
            throw new BusinessException("图形验证服务异常，请稍后重试");
        }
    }
}
