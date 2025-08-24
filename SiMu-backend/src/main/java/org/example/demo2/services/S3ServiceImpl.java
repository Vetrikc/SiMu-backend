package org.example.demo2.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Service for interacting with AWS S3 to upload, download, and delete files.
 */
@Service
public class S3ServiceImpl implements S3Service {
    private static final Logger logger = LoggerFactory.getLogger(S3ServiceImpl.class);

    @Autowired
    private S3Client s3Client;

    @Value("${s3client.endpoint}")
    private String s3ClientEndpoint;

    @Value("${s3client.bucket.music}")
    private String musicBucket;

    /**
     * Uploads a file to the configured S3 bucket and returns the file URL.
     *
     * @param key  the key (path) under which the file will be stored in S3
     * @param file the file to upload
     * @return the URL of the uploaded file
     * @throws IllegalArgumentException if key is invalid or file does not exist
     * @throws S3Exception             if the upload fails
     */
    public String uploadFile(String key, File file) {
        validateKey(key);
        validateFile(file);

        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(musicBucket)
                    .key(key)
                    .build(),
                    file.toPath());

            String fileUrl = String.format("%s/%s/%s", s3ClientEndpoint, musicBucket, key);
            logger.info("File uploaded to bucket: {}, URL: {}", musicBucket, fileUrl);
            return fileUrl;
        } catch (S3Exception e) {
            logger.error("Failed to upload file to S3: bucket={}, key={}", musicBucket, key, e);
            throw e;
        }
    }

    /**
     * Downloads a file from the specified S3 bucket to the destination path.
     *
     * @param bucketName      the name of the S3 bucket
     * @param key             the key of the file in S3
     * @param destinationPath the local path to save the file
     * @return the destination path of the downloaded file
     * @throws IllegalArgumentException if parameters are invalid
     * @throws S3Exception             if the download fails
     */
    public Path downloadFile(String bucketName, String key, Path destinationPath) {
        validateBucketName(bucketName);
        validateKey(key);
        Objects.requireNonNull(destinationPath, "Destination path must not be null");

        try {
            GetObjectResponse response = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build(),
                    destinationPath
            );

            logger.info("File downloaded from bucket: {}, key: {}, to: {}", bucketName, key, destinationPath);
            return destinationPath;

        } catch (S3Exception e) {
            logger.error("Failed to download file from S3: bucket={}, key={}", bucketName, key, e);
            throw e;
        }
    }


    /**
     * Deletes a file from the specified S3 bucket.
     *
     * @param bucketName the name of the S3 bucket
     * @param key        the key of the file to delete
     * @throws IllegalArgumentException if parameters are invalid
     * @throws S3Exception             if the deletion fails
     */
    public void deleteFile(String bucketName, String key) {
        validateBucketName(bucketName);
        validateKey(key);

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            logger.info("File deleted from bucket: {}, key: {}", bucketName, key);
        } catch (S3Exception e) {
            logger.error("Failed to delete file from S3: bucket={}, key={}", bucketName, key, e);
            throw e;
        }
    }

    private void validateKey(String key) {
        Objects.requireNonNull(key, "Key must not be null");
        if (key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
    }

    private void validateBucketName(String bucketName) {
        Objects.requireNonNull(bucketName, "Bucket name must not be null");
        if (bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bucket name must not be empty");
        }
    }

    private void validateFile(File file) {
        Objects.requireNonNull(file, "File must not be null");
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File does not exist or is not a valid file");
        }
    }

}
