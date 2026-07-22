package com.hmdp.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {
    private String nickName;
    private String icon;
    private String gender;
    private String birthday;
    private String city;
    private String introduce;
}
