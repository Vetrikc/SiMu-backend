package org.example.demo2.services;


import java.io.File;
import java.nio.file.Path;

public interface S3Service {

    public String uploadFile(String key, File file);

    public Path downloadFile(String bucketName, String key, Path destinationPath);

    public void deleteFile(String bucketName, String key);

}
