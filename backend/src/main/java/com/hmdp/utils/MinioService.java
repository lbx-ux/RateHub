package com.hmdp.utils;

import com.hmdp.config.MinioProperties;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 上传文件（公有桶）
     *
     * @return 返回文件的完整公开访问 URL
     */
    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";

        // 使用 UUID 生成唯一文件名，防止重名冲突
        String objectName = UUID.randomUUID().toString().replace("-", "") + extension;

        // 合并了 try 块，更简洁
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType()) // 必须指定类型，否则浏览器访问时一律变成下载
                            .build()
            );
            log.info("文件上传成功: {}", objectName);

            // 公有桶场景下，上传成功后直接返回完整的访问 URL 给前端
            return getFileUrl(objectName);

        } catch (Exception e) {
            log.error("MinIO 上传文件失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件的公开访问 URL（替代了私有桶的预签名方法）
     *
     * @param objectName 文件名
     * @return 完整的 URL 字符串
     */
    public String getFileUrl(String objectName) {
        String endpoint = minioProperties.getEndpoint();
        // 容错处理：确保 endpoint 结尾没有多余的斜杠
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + minioProperties.getBucketName() + "/" + objectName;
    }

    /**
     * 删除文件
     * 注意：即便桶是公开读的，删除等写入操作依然需要验证，所以此代码与私有桶完全一致
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
            log.info("文件删除成功: {}", objectName);
        } catch (Exception e) {
            log.error("MinIO 删除文件失败", e);
            throw new RuntimeException("文件删除失败");
        }
    }

}