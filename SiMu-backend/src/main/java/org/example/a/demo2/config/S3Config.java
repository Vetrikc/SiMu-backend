package org.example.a.demo2.config;

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

    @Bean
    public S3Client s3Client() {
        String accessKey = "ARNC4NENR274L89169FI";
        String secretKey = "VtgohLBWy2RTqAER6ejh0oFwU0IA8qvBSpDAqo3t";


        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .region(Region.of("ru-central1")) // Укажите регион Timeweb
                .endpointOverride(java.net.URI.create(endpoint)) // Настройка кастомного эндпоинта
                .build();
    }
}
