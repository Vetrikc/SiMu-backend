package org.example.demo2.config;

import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {
    @Value("${s3client.endpoint}")
    protected String endpoint;
    @Value("${s3client.accessKey}")
    protected String accessKey;
    @Value("${s3client.secretKey}")
    protected String secretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of("ru-1")) // Укажите регион Timeweb
                .endpointOverride(java.net.URI.create(endpoint)) // Настройка кастомного эндпоинта
                .build();
    }
}
