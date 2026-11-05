package org.example.simu.music.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.util.Objects;

/**
 * Thin wrapper around the S3 client: upload, delete and URL construction for the music bucket.
 */
@Service
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final String endpoint;
    private final String musicBucket;

    public S3Service(S3Client s3Client,
                     @Value("${s3client.endpoint}") String endpoint,
                     @Value("${s3client.bucket.music}") String musicBucket) {
        this.s3Client = s3Client;
        this.endpoint = endpoint;
        this.musicBucket = musicBucket;
    }

    public String uploadFile(String key, File file) {
        validateKey(key);
        validateFile(file);
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(musicBucket)
                    .key(key)
                    .build(), file.toPath());
            String fileUrl = String.format("%s/%s/%s", endpoint, musicBucket, key);
            log.info("File uploaded to bucket: {}, URL: {}", musicBucket, fileUrl);
            return fileUrl;
        } catch (S3Exception e) {
            log.error("Failed to upload file to S3: bucket={}, key={}", musicBucket, key, e);
            throw e;
        }
    }

    public void deleteFile(String key) {
        validateKey(key);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(musicBucket)
                    .key(key)
                    .build());
            log.info("File deleted from bucket: {}, key: {}", musicBucket, key);
        } catch (S3Exception e) {
            log.error("Failed to delete file from S3: bucket={}, key={}", musicBucket, key, e);
            throw e;
        }
    }

    private void validateKey(String key) {
        Objects.requireNonNull(key, "Key must not be null");
        if (key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
    }

    private void validateFile(File file) {
        Objects.requireNonNull(file, "File must not be null");
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File does not exist or is not a valid file");
        }
    }
}
