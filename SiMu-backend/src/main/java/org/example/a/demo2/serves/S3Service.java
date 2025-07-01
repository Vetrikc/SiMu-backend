package org.example.a.demo2.serves;



import java.io.File;
import java.nio.file.Path;

public interface S3Service {

    public String uploadFile(String bucketName, String key, File file) ;

    public Path downloadFile(String bucketName, String key, Path destinationPath) ;

    public void deleteFile(String bucketName, String key) ;

}
