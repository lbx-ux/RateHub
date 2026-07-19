# RateHub 前后端交互接口文档

本文档定义了 RateHub 移动端应用与后端服务交互的 API 接口规范。所有接口的基础 URL 默认为 `/api`，鉴权 Token 在请求头中以 `authorization` 传输。

---

## 目录
1. [用户模块](#1-用户模块)
2. [商铺模块](#2-商铺模块)
3. [优惠券及秒杀模块](#3-优惠券及秒杀模块)
4. [探店日记模块](#4-探店日记模块)
5. [社交关系模块](#5-社交关系模块)

---

## 1. 用户模块

### 1.1 发送短信验证码
- **接口名称**：发送短信验证码
- **接口描述**：向用户手机号发送用于登录或注册的 6 位数字验证码。
- **请求 URL**：`/user/code`
- **请求方式**：`POST`
- **请求参数 (Query)**：
  | 参数名 | 类型 | 是否必填 | 示例值 | 说明 |
  | :--- | :--- | :--- | :--- | :--- |
  | `phone` | `String` | 是 | `13812345678` | 用户接收验证码的手机号 |

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": null
  }
  ```

---

### 1.2 用户登录（手机号验证码/密码登录）
- **接口名称**：用户登录/注册
- **接口描述**：支持验证码快速登录（若未注册则自动注册）或传统手机号密码登录。登录成功后返回 Token。
- **请求 URL**：`/user/login`
- **请求方式**：`POST`
- **请求参数 (Request Body)**：
  | 参数名 | 类型 | 是否必填 | 示例值 | 说明 |
  | :--- | :--- | :--- | :--- | :--- |
  | `phone` | `String` | 是 | `13812345678` | 手机号 |
  | `code` | `String` | 否 | `123456` | 短信验证码（验证码登录时必填） |
  | `password` | `String` | 否 | `123456` | 密码（密码登录时必填） |

- **请求报文示例**：
  ```json
  {
    "phone": "13812345678",
    "code": "123456"
  }
  ```

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": "session-token-string-uuid-xxxx-xxxx"
  }
  ```
  > [!NOTE]
  > 返回的 `data` 字符串即为用户的 `Token`，前端需将其存入本地会话存储（如 `sessionStorage`），并在后续所有请求的 Request Headers 中携带 `authorization` 字段。

---

### 1.3 获取当前登录用户基本信息
- **接口名称**：获取当前登录用户
- **接口描述**：通过请求头的 Token 识别并返回当前登录用户的脱敏基本信息。
- **请求 URL**：`/user/me`
- **请求方式**：`GET`
- **请求头 (Request Headers)**：
  | 参数名 | 参数值 | 是否必填 | 说明 |
  | :--- | :--- | :--- | :--- |
  | `authorization` | `session-token-string-uuid-xxxx-xxxx` | 是 | 登录时获取的 JWT 或 Session Token |

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 1001,
      "nickName": "用户_3a8b",
      "icon": "/imgs/icons/default-icon.png"
    }
  }
  ```

---

### 1.4 查询用户详细资料
- **接口名称**：查询用户详情
- **接口描述**：根据用户 ID 获取该用户的社交详情资料（性别、城市、介绍等）。
- **请求 URL**：`/user/info/{userId}`
- **请求方式**：`GET`

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": {
      "userId": 1001,
      "introduce": "美食达人，吃遍杭州！",
      "gender": "男",
      "city": "杭州",
      "birthday": "1998-05-20"
    }
  }
  ```

---

### 1.5 退出登录
- **接口名称**：退出登录
- **接口描述**：清除服务器端保存的登录 Token。
- **请求 URL**：`/user/logout`
- **请求方式**：`POST`
- **请求头 (Request Headers)**：
  | 参数名 | 参数值 | 是否必填 | 说明 |
  | :--- | :--- | :--- | :--- |
  | `authorization` | `session-token-string-uuid-xxxx-xxxx` | 是 | 会话 Token |

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": null
  }
  ```

---

## 2. 商铺模块

### 2.1 查询商铺类型列表
- **接口名称**：查询商铺类型
- **接口描述**：获取首页商铺的大类列表，包含分类名称及图标。
- **请求 URL**：`/shop-type/list`
- **请求方式**：`GET`

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 1,
        "name": "美食",
        "icon": "type-food.png",
        "sort": 1
      },
      {
        "id": 2,
        "name": "KTV",
        "icon": "type-ktv.png",
        "sort": 2
      }
    ]
  }
  ```

---

### 2.2 分页条件查询商铺列表
- **接口名称**：分页条件查询商铺
- **接口描述**：根据商铺分类、排序规则及用户当前地理位置，查询附近的商铺。
- **请求 URL**：`/shop/of/type`
- **请求方式**：`GET`
- **请求参数 (Query)**：
  | 参数名 | 类型 | 是否必填 | 示例值 | 说明 |
  | :--- | :--- | :--- | :--- | :--- |
  | `typeId` | `Long` | 是 | `1` | 商铺分类 ID |
  | `current` | `Integer` | 是 | `1` | 当前页码 |
  | `sortBy` | `String` | 否 | `comments` | 排序字段（支持 `comments`-人气, `score`-评分, 空值则按距离排序） |
  | `x` | `Double` | 否 | `120.149993` | 用户当前经度 |
  | `y` | `Double` | 否 | `30.334229` | 用户当前纬度 |

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 2001,
        "name": "西湖醋鱼楼",
        "images": "https://img.example.com/fish.jpg",
        "score": 48,
        "comments": 150,
        "avgPrice": 98,
        "area": "西湖景区",
        "distance": 850.5,
        "address": "杭州市西湖区孤山路1号"
      }
    ]
  }
  ```
  > [!NOTE]
  > `score` 字段乘以 0.1 即为展示的评分星级（例如 `48` 对应星级 `4.8`）。

---

### 2.3 根据ID获取商铺详情
- **接口名称**：查询商铺详情
- **接口描述**：根据商铺 ID 获取商铺的所有详细配置字段。
- **请求 URL**：`/shop/{id}`
- **请求方式**：`GET`

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 2001,
      "name": "西湖醋鱼楼",
      "images": "https://img.example.com/fish1.jpg,https://img.example.com/fish2.jpg",
      "score": 48,
      "comments": 150,
      "avgPrice": 98,
      "area": "西湖景区",
      "address": "杭州市西湖区孤山路1号",
      "openHours": "10:00-22:00"
    }
  }
  ```

---

## 3. 优惠券及秒杀模块

### 3.1 查询商铺优惠券列表
- **接口名称**：查询商铺优惠券
- **接口描述**：获取指定商铺下的代金券与限时抢购（秒杀）优惠券。
- **请求 URL**：`/voucher/list/{shopId}`
- **请求方式**：`GET`

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 101,
        "title": "100元代金券",
        "subTitle": "全场通用",
        "payValue": 9000,
        "actualValue": 10000,
        "type": 0
      },
      {
        "id": 102,
        "title": "50元限量秒杀券",
        "subTitle": "限时抢购",
        "payValue": 1000,
        "actualValue": 5000,
        "type": 1,
        "stock": 50,
        "beginTime": "2026-07-03T20:00:00",
        "endTime": "2026-07-03T22:00:00"
      }
    ]
  }
  ```
  > [!NOTE]
  > `payValue` 和 `actualValue` 的单位为“分”（例如 `9000` 表示 90 元）。`type = 1` 代表是限时秒杀券。

---

### 3.2 秒杀优惠券下单抢购
- **接口名称**：秒杀优惠券下单
- **接口描述**：在高并发场景下提交抢购秒杀优惠券请求。
- **请求 URL**：`/voucher-order/seckill/{id}`
- **请求方式**：`POST`
- **请求头 (Request Headers)**：
  | 参数名 | 参数值 | 是否必填 | 说明 |
  | :--- | :--- | :--- | :--- |
  | `authorization` | `session-token-string-uuid-xxxx-xxxx` | 是 | 登录凭证 |

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": 9876543210123
  }
  ```
  > [!NOTE]
  > 抢购成功时，返回的 `data` 为新建的秒杀订单唯一 ID (Long)。

---

## 4. 探店日记模块

### 4.1 分页查询热门探店日记
- **接口名称**：分页查询热门日记
- **接口描述**：在首页或推荐流展示当前最受欢迎（点赞数多）的笔记列表。
- **请求 URL**：`/blog/hot`
- **请求方式**：`GET`
- **请求参数 (Query)**：
  | 参数名 | 类型 | 是否必填 | 示例值 | 说明 |
  | :--- | :--- | :--- | :--- | :--- |
  | `current` | `Integer` | 是 | `1` | 当前页码 |

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 501,
        "userId": 1001,
        "name": "小明说吃",
        "icon": "/imgs/avatar/xm.png",
        "title": "宝藏杭州菜馆打卡！",
        "images": "https://img.example.com/blog1.jpg",
        "liked": 23,
        "isLike": false
      }
    ]
  }
  ```

---

### 4.2 按ID查询日记详情
- **接口名称**：根据ID查询日记
- **接口描述**：获取日记的详细内容，并查询当前访问用户是否对该日记点过赞。
- **请求 URL**：`/blog/{id}`
- **请求方式**：`GET`

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 501,
      "userId": 1001,
      "name": "小明说吃",
      "icon": "/imgs/avatar/xm.png",
      "title": "宝藏杭州菜馆打卡！",
      "content": "今天终于打卡了这家被疯狂安利的馆子，菜品美味十足，环境极有江南风韵...",
      "images": "https://img.example.com/blog1.jpg,https://img.example.com/blog2.jpg",
      "liked": 23,
      "isLike": true,
      "shopId": 2001,
      "createTime": "2026-07-03T12:00:00"
    }
  }
  ```

---

### 4.3 笔记图片上传
- **接口名称**：上传笔记图片
- **接口描述**：在发日记时上传单张图片，后端保存并返回图片访问 URL 路径。
- **请求 URL**：`/upload/blog`
- **请求方式**：`POST`
- **请求头 (Request Headers)**：
  | 参数名 | 参数值 | 是否必填 | 说明 |
  | :--- | :--- | :--- | :--- |
  | `Content-Type` | `multipart/form-data` | 是 | 声明为多部分表单格式 |
- **请求参数 (Form Data)**：
  | 参数名 | 类型 | 是否必填 | 示例值 | 说明 |
  | :--- | :--- | :--- | :--- | :--- |
  | `file` | `File` | 是 | `[Binary]` | 图片二进制文件 |

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": "/blogs/upload_abcd123.jpg"
  }
  ```

---

### 4.4 删除上传的笔记图片
- **接口名称**：删除笔记图片
- **接口描述**：发布前撤销上传某张图片，以在服务端清理文件占用的空间。
- **请求 URL**：`/upload/blog/delete`
- **请求方式**：`GET`
- **请求参数 (Query)**：
  | 参数名 | 类型 | 是否必填 | 示例值 | 说明 |
  | :--- | :--- | :--- | :--- | :--- |
  | `name` | `String` | 是 | `/imgs/blogs/upload_abcd123.jpg` | 要删除的图片完整相对路径 |

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": null
  }
  ```

---

### 4.5 发布探店笔记
- **接口名称**：发布笔记
- **接口描述**：提交笔记的标题、内容、关联商户及关联图片链接以正式发布。
- **请求 URL**：`/blog`
- **请求方式**：`POST`
- **请求头 (Request Headers)**：
  | 参数名 | 参数值 | 是否必填 | 说明 |
  | :--- | :--- | :--- | :--- |
  | `authorization` | `session-token-string-uuid-xxxx-xxxx` | 是 | 用户登录 Token |

- **请求参数 (Request Body)**：
  ```json
  {
    "title": "美味打卡",
    "content": "这家烤肉店绝了，环境优雅服务非常周到！",
    "images": "/imgs/blogs/upload_1.jpg,/imgs/blogs/upload_2.jpg",
    "shopId": 2001
  }
  ```

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": 502
  }
  ```

---

### 4.6 笔记点赞/取消点赞
- **接口名称**：笔记点赞/取消点赞
- **接口描述**：对探店日记执行点赞（如果是首次点赞）或取消点赞（如果已赞过）的操作。
- **请求 URL**：`/blog/like/{id}`
- **请求方式**：`PUT`
- **请求头 (Request Headers)**：
  | 参数名 | 参数值 | 是否必填 | 说明 |
  | :--- | :--- | :--- | :--- |
  | `authorization` | `session-token-string-uuid-xxxx-xxxx` | 是 | 登录凭据 |

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": null
  }
  ```

---

### 4.7 查询点赞用户列表
- **接口名称**：查询点赞用户
- **接口描述**：获取最先给该日记点赞的 5 位用户的基本资料（用于在日记详情中显示点赞头像列表）。
- **请求 URL**：`/blog/likes/{id}`
- **请求方式**：`GET`

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 1002,
        "nickName": "吃货小红",
        "icon": "/imgs/avatar/xh.png"
      },
      {
        "id": 1003,
        "nickName": "阿强爱吃饭",
        "icon": "/imgs/avatar/aq.png"
      }
    ]
  }
  ```

---

### 4.8 滚动加载已关注达人的笔记（推送模式）
- **接口名称**：获取关注者笔记
- **接口描述**：在“关注”Tab页中滚动加载用户所关注达人的最新发布的探店日记。
- **请求 URL**：`/blog/of/follow`
- **请求方式**：`GET`
- **请求参数 (Query)**：
  | 参数名 | 类型 | 是否必填 | 示例值 | 说明 |
  | :--- | :--- | :--- | :--- | :--- |
  | `offset` | `Integer` | 是 | `0` | 上次拉取的最后一个元素和当前最小时间戳相同的偏移量个数 |
  | `lastId` | `Long` | 是 | `1688382900000` | 上次查询返回的最后一条笔记的发布时间戳 |

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": {
      "list": [
        {
          "id": 503,
          "title": "最新烧烤推荐",
          "images": "https://img.example.com/sk.jpg",
          "name": "烤肉大佬",
          "icon": "/imgs/avatar/dl.png",
          "liked": 88
        }
      ],
      "minTime": 1688381800000,
      "offset": 1
    }
  }
  ```

---

## 5. 社交关系模块

### 5.1 关注或取关达人
- **接口名称**：关注达人
- **接口描述**：关注或取消关注某位发布探店笔记的达人。
- **请求 URL**：`/follow/{userId}/{isFollow}`
- **请求方式**：`PUT`
- **请求参数 (Path)**：
  | 参数名 | 类型 | 是否必填 | 示例值 | 说明 |
  | :--- | :--- | :--- | :--- | :--- |
  | `userId` | `Long` | 是 | `1001` | 目标用户 ID |
  | `isFollow` | `Boolean` | 是 | `true` | `true`-关注, `false`-取关 |

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": null
  }
  ```

---

### 5.2 查询是否已关注目标用户
- **接口名称**：查询关注状态
- **接口描述**：检测当前登录用户与指定目标用户的关注纽带。
- **请求 URL**：`/follow/or/not/{userId}`
- **请求方式**：`GET`

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": true
  }
  ```

---

### 5.3 查询与他人的共同关注列表
- **接口名称**：共同关注
- **接口描述**：查询当前用户与目标用户同时关注的达人交集列表。
- **请求 URL**：`/follow/common/{id}`
- **请求方式**：`GET`

- **响应数据示例 (成功)**：
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 1005,
        "nickName": "大胃王阿豪",
        "icon": "/imgs/avatar/ah.png"
      }
    ]
  }
  ```

---

## 6. 通用返回结构与状态码

### 6.1 通用响应格式说明
后端所有接口使用统一返回对象体封装 Result<T>：
```json
{
  "code": 200,       // 响应状态码 (200: 成功, 非200: 失败)
  "message": "操作成功", // 提示信息 (成功或失败提示)
  "data": {},        // 业务核心载荷
  "total": null      // 分页总条数 (可选，仅在分页返回时非空)
}
```

### 6.2 异常响应格式
当请求失败或发生业务异常时，data 始终为 null：
```json
{
  "code": 500,
  "message": "系统内部异常",
  "data": null,
  "total": null
}
```

### 6.3 统一业务状态码 (ResultCode)
- 200 (SUCCESS)：操作成功。
- 400 (PARAM_ERROR)：参数校验失败。
- 401 (UNAUTHORIZED)：暂未登录或token已经过期。
- 403 (FORBIDDEN)：没有相关权限。
- 500 (ERROR)：系统内部异常（如数据库断连、未知系统异常）。
