package com.hmdp.dto;

import com.hmdp.entity.Shop;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 店铺列表展示 VO，在 Shop 基础上追加是否含有优惠券的标志位。
 * 仅用于列表查询场景的数据传输，不修改原始实体类。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShopVO extends Shop {

    /**
     * 是否有可用优惠券（普通券或秒杀券）
     * true  – 店内存在有效优惠券
     * false – 没有优惠券
     */
    private Boolean hasVoucher;
}
