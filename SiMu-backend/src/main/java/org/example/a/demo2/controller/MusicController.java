package org.example.a.demo2.controller;

import org.example.a.demo2.entity.Music;
import org.example.a.demo2.serves.MusicServes;
import org.example.a.demo2.serves.S3ServiceImplimentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MusicController {

    @Autowired
    private MusicServes musicServes;

    @Autowired
    S3ServiceImplimentation s3ServiceImplimentation;

    @PostMapping("/Musics/{id}")
    public Music getMusics(@PathVariable Long id) {
        return musicServes.getMusic(id);
    }

    @GetMapping("/musics")
    public List<Music> getMusicList() {
        return musicServes.getAllMusic();
    }

    @PostMapping("/musics")
    public String uploadFile(@RequestPart("multipartFile") MultipartFile multipartFile, @RequestPart Music music) {
        String bucketName = "50e8fbd1-6b3efd64-7616-4b97-9837-f58345923ecb";
        System.out.println(music.toString());
        String key = multipartFile.getOriginalFilename();
        System.out.println("Файл получен: " + key);
        try {
            // Create a temporary file
            File file = File.createTempFile("temp", music.getName() + "_" + music.getArtist() +
                    "." + multipartFile.getOriginalFilename().
                    split("\\.")[multipartFile.getOriginalFilename().split("\\.").length - 1]);

            // Transfer the content of the MultipartFile to the newly created file
            multipartFile.transferTo(file);

            String fileUrl = s3ServiceImplimentation.uploadFile(bucketName, music.getName() + "_" + music.getArtist() +
                    "." + multipartFile.getOriginalFilename().
                    split("\\.")[multipartFile.getOriginalFilename().split("\\.").length - 1], file);
            music.setUrl(fileUrl);
            musicServes.seveMusic(music);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return "Файл " + key + " успешно загружен в бакет " + bucketName;
    }
    @GetMapping("/protected")
    public ResponseEntity<String> protectedEndpoint() {
        return ResponseEntity.ok("Success");
    }

}

