package com.hmdp.dto;

import lombok.Data;

@Data
public class CaptchaCodeDTO {
    private String phone;
    private String lotNumber;
    private String captchaOutput;
    private String passToken;
    private String genTime;
}
