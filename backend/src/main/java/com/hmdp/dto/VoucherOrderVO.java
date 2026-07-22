package com.hmdp.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoucherOrderVO {
    private Long id;
    private Long shopId;
    private Long voucherId;
    private String title;
    private Long payValue;
    private Long actualValue;
    private Integer type;
    private Integer status;
    private LocalDateTime createTime;
}
