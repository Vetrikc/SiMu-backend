/*
package org.example.a.demo2.controller;

import org.example.a.demo2.serves.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/s3")
public class S3Controller {

    @Autowired
    private S3Service s3Service;



    @GetMapping("/download")
    public String downloadFile(@RequestParam String key, @RequestParam String destinationPath) {
        String bucketName = "50e8fbd1-6b3efd64-7616-4b97-9837-f58345923ecb";
        Path path = Paths.get(destinationPath);
        s3Service.downloadFile(bucketName, key, path);
        return "Файл " + key + " скачан в " + destinationPath;
    }

    @DeleteMapping("/delete")
    public String deleteFile(@RequestParam String key) {
        String bucketName = "50e8fbd1-6b3efd64-7616-4b97-9837-f58345923ecb";
        s3Service.deleteFile(bucketName, key);
        return "Файл " + key + " удален из бакета " + bucketName;
    }


    public static MultipartFile convertToMultipartFile(byte[] fileContent, String fileName, String contentType) {
        return new MockMultipartFile(
                fileName,            // Имя файла
                fileName,            // Оригинальное имя файла
                contentType,         // Тип контента (например, "application/pdf")
                fileContent          // Содержимое файла
        );
    }


}
*/
