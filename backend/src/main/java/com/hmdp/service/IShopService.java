package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.dto.ShopVO;
import com.hmdp.entity.Shop;

import java.util.List;

public interface IShopService {

    Shop getById(Long id);

    void save(Shop shop);

    void updateById(Shop shop);

    List<ShopVO> queryShopByType(Integer typeId, int current, int pageSize);

    List<ShopVO> queryShopByName(String name, int current, int pageSize);

    Result<Shop> queryShopById(Long id);
}
