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
 * S3-compatible object storage (Timeweb / MinIO / AWS) used for the audio files.
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

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .build();
    }
}
