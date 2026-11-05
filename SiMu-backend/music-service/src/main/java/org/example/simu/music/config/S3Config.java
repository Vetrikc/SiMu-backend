package org.example.simu.music.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * S3-compatible object storage.
 * <p>
 * Works both against a managed S3 (Timeweb, AWS) and against MinIO running next to the stack:
 * {@code forcePathStyle} makes the SDK use {@code http://host:9000/bucket/key} instead of the
 * virtual-host style {@code http://bucket.host:9000/key}, which MinIO does not serve for arbitrary
 * host names.
 */
@Configuration
public class S3Config {

    @Value("${s3client.endpoint}")
    private String endpoint;

    @Value("${s3client.accessKey}")
    private String accessKey;

    @Value("${s3client.secretKey}")
    private String secretKey;

    @Value("${s3client.region:ru-1}")
    private String region;

    @Value("${s3client.path-style:true}")
    private boolean pathStyle;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .forcePathStyle(pathStyle)
                .build();
    }
}
