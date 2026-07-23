package com.hmdp.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties minioProperties){
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
        try{
            String bucketName = minioProperties.getBucketName();
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!found) {
                // 1. 创建 Bucket
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );

                // 2. 赋予公开读权限 (Public Policy)
                String policyJson = "{\n" +
                        "  \"Statement\": [\n" +
                        "    {\n" +
                        "      \"Action\": \"s3:GetObject\",\n" +
                        "      \"Effect\": \"Allow\",\n" +
                        "      \"Principal\": \"*\",\n" +
                        "      \"Resource\": \"arn:aws:s3:::" + bucketName + "/*\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"Version\": \"2012-10-17\"\n" +
                        "}";

                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .config(policyJson)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO 自动初始化 Bucket 失败，请检查网络或配置！", e);
        }
        return minioClient;
    }

}
