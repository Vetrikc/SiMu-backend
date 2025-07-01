package org.example.a.demo2.serves;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.File;
import java.nio.file.Path;

@Service
public class S3ServiceImplimentation {

    @Autowired
    private S3Client s3Client;

    @Value("${s3client.endpoint}")
    protected String S3ClientEnpoint;

    public String uploadFile(String bucketName, String key, File file) {
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build(),
                file.toPath());
        String urlToFile = S3ClientEnpoint +"/"+ bucketName + "/" + key;
        System.out.println("Файл загружен в бакет: " + bucketName);
        System.out.println("Путь " + urlToFile);
        return urlToFile;
    }

    public Path downloadFile(String bucketName, String key, Path destinationPath) {
        s3Client.getObject(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build(),
                destinationPath);
        return destinationPath;
    }

    public void deleteFile(String bucketName, String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
        System.out.println("Файл удален из бакета: " + bucketName);
    }

}
