package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;

import java.util.List;

public interface IShopTypeService {

    Result<List<ShopType>> queryTypeList();
}
