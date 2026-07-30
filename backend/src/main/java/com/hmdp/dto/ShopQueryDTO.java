package com.hmdp.dto;

import lombok.Data;

@Data
public class ShopQueryDTO {
    private Integer typeId;
    private Integer current = 1;
    private String sortBy;
    private Integer distance;
    // 写死默认坐标（杭州市拱墅区），方便前端未传坐标时测试 GEO 功能
    private Double x = 120.149192;
    private Double y = 30.316078;
}
