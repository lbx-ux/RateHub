package com.hmdp.controller;

import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import com.hmdp.utils.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("upload")
public class UploadController {

    private final MinioService minioService;

    @PostMapping("blog")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        String url = minioService.uploadFile(image);
        return Result.success(url);
    }

    @PostMapping("avatar")
    public Result uploadAvatar(@RequestParam("file") MultipartFile image) {
        String url = minioService.uploadFile(image);
        return Result.success(url);
    }

    @GetMapping("/blog/delete")
    public Result deleteBlogImg(@RequestParam("name") String filename) {
        // filename 可能是完整 URL，提取对象名
        String objectName = filename;
        if (StrUtil.isNotBlank(filename) && filename.contains("/")) {
            objectName = filename.substring(filename.lastIndexOf("/") + 1);
        }
        minioService.deleteFile(objectName);
        return Result.success();
    }
}
