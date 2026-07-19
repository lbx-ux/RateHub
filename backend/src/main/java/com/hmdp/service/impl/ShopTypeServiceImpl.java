package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.exception.BusinessException;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopTypeServiceImpl implements IShopTypeService {
    private final ShopTypeMapper shopTypeMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<List<ShopType>> queryTypeList() {
        String key="shop:type";
        String shopTypeJson=stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(shopTypeJson)){
            return Result.success(JSONUtil.toList(shopTypeJson, ShopType.class));
        }
        List<ShopType> shopTypeList=shopTypeMapper.queryTypeList();
        if(shopTypeList==null){
            throw new BusinessException("店铺不存在");
        }
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopTypeList));
        return Result.success(shopTypeList);
    }
}
