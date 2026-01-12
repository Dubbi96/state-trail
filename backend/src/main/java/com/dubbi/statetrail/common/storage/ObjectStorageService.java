package com.dubbi.statetrail.common.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * MinIO 객체 스토리지 서비스
 * 스크린샷, 네트워크 로그, trace 등 증거물 저장
 */
@Service
public class ObjectStorageService {
    private final MinioClient minioClient;
    private final String bucketName;

    public ObjectStorageService(
            @Value("${storage.minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${storage.minio.access-key:minioadmin}") String accessKey,
            @Value("${storage.minio.secret-key:minioadmin}") String secretKey,
            @Value("${storage.minio.bucket:statetrail}") String bucketName
    ) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucketName = bucketName;
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            System.err.println("Failed to ensure bucket exists: " + e.getMessage());
        }
    }

    /**
     * 스크린샷 저장 (PNG 바이트 배열)
     * @return objectKey (예: "screenshots/{runId}/{pageId}.png")
     */
    public String saveScreenshot(UUID runId, UUID pageId, byte[] screenshotBytes) {
        try {
            String objectKey = String.format("screenshots/%s/%s.png", runId, pageId);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(screenshotBytes), screenshotBytes.length, -1)
                    .contentType("image/png")
                    .build());
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save screenshot: " + e.getMessage(), e);
        }
    }

    /**
     * 네트워크 로그 저장 (HAR JSON)
     * @return objectKey (예: "network-logs/{runId}/{pageId}.har")
     */
    public String saveNetworkLog(UUID runId, UUID pageId, String harJson) {
        try {
            String objectKey = String.format("network-logs/%s/%s.har", runId, pageId);
            byte[] bytes = harJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType("application/json")
                    .build());
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save network log: " + e.getMessage(), e);
        }
    }

    /**
     * Storage state 저장 (Playwright storage state JSON)
     * @return objectKey (예: "storage-states/{authProfileId}/{timestamp}.json")
     */
    public String saveStorageState(UUID authProfileId, String storageStateJson) {
        try {
            String objectKey = String.format("storage-states/%s/%s.json", authProfileId, UUID.randomUUID());
            byte[] bytes = storageStateJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType("application/json")
                    .build());
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save storage state: " + e.getMessage(), e);
        }
    }

    /**
     * Storage state 로드 (InputStream 반환)
     */
    public InputStream loadStorageState(String objectKey) {
        try {
            return minioClient.getObject(io.minio.GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load storage state: " + e.getMessage(), e);
        }
    }

    /**
     * Presigned URL 생성 (읽기 전용, 1시간 유효)
     */
    public String getPresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(io.minio.GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.GET)
                    .bucket(bucketName)
                    .object(objectKey)
                    .expiry(60 * 60) // 1 hour
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
        }
    }
}

