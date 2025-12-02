package com.z.c.woodexcess_api.service.storage;

import com.z.c.woodexcess_api.exception.storage.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public String upload(MultipartFile file, String folder) throws IOException {
        log.info("Starting S3 upload: file={}, size={} bytes, folder={}",
                file.getOriginalFilename(), file.getSize(), folder);

        try {
            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            String key = folder + "/" + fileName;

            log.debug("Generated S3 key: {}", key);
            log.debug("Target bucket: {}", bucketName);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromBytes(file.getBytes()));

            log.info("S3 upload successful: key={}, size={} bytes", key, file.getSize());
            return key;

        } catch (S3Exception e) {
            log.error("S3 upload failed: errorCode={}, message={}",
                    e.awsErrorDetails().errorCode(),
                    e.awsErrorDetails().errorMessage(),
                    e);
            throw new FileStorageException("Failed to upload file to S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            throw new FileStorageException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPubliUrl(String key) {
        String url = String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, key);
        log.debug("Generated public URL: {}", url);
        return url;
    }

    @Override
    public String getPressignUrl(String key, int durationMinutes) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(durationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key {}: {}", key, e.getMessage(), e);
            throw new FileStorageException("Failed to generate presigned URL", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            log.info("Deleting S3 object: {}", key);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 object deleted successfully: {}", key);

        } catch (S3Exception e) {
            log.error("S3 delete operation failed for key {}: {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new FileStorageException("Failed to delete file from S3", e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("Failed to check file existence for key {}: {}", key, e.awsErrorDetails().errorMessage(), e);
            return false;
        }
    }
}
