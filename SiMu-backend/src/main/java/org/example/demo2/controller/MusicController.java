package org.example.demo2.controller;

import jakarta.validation.Valid;

import org.example.demo2.dto.MusicDTO;
import org.example.demo2.dto.MusicUploadDTO;
import org.example.demo2.services.MusicService;
import org.example.demo2.services.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

/**
 * REST controller for managing music-related operations.
 */
@RestController
@RequestMapping("/api/music")
public class MusicController {
    private static final Logger logger = LoggerFactory.getLogger(MusicController.class);

    @Autowired
    private MusicService musicService;

    @Autowired
    private S3Service s3Service;

    @GetMapping("/{id}")
    public ResponseEntity<MusicDTO> getMusic(@PathVariable Long id) {
        logger.debug("Fetching music with id: {}", id);
        MusicDTO musicDTO = musicService.getMusic(id);
        return ResponseEntity.ok(musicDTO);
    }

    @GetMapping
    public ResponseEntity<List<MusicDTO>> getAllMusic() {
        logger.debug("Fetching all music records");
        List<MusicDTO> musicList = musicService.getAllMusic();
        return ResponseEntity.ok(musicList);
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<MusicDTO> uploadMusic(
            @RequestPart("file") MultipartFile file,
            @RequestPart("music") @Valid MusicUploadDTO musicDTO) {
        Objects.requireNonNull(file, "File must not be null");
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File name must not be empty");
        }

        logger.info("Received file upload request: filename={}", originalFilename);

        File tempFile = null;
        try {
            // Extract file extension
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            String s3Key = String.format("%s_%s.%s", musicDTO.getName(), musicDTO.getArtist(), extension);

            // Create temporary file
            tempFile = File.createTempFile("music_", "." + extension);
            file.transferTo(tempFile);

            // Upload to S3
            String fileUrl = s3Service.uploadFile(s3Key, tempFile);
            MusicDTO musicDTO1 = new MusicDTO();
            musicDTO1.setArtist(musicDTO.getArtist());
            musicDTO1.setName(musicDTO.getName());
            musicDTO1.setUrl(fileUrl);
            musicDTO1.setId(null); // Ensure new entity is created

            // Save to database
            MusicDTO savedMusicDTO = musicService.saveMusic(musicDTO1);
            logger.info("Music uploaded and saved: id={}, url={}", savedMusicDTO.getId(), fileUrl);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedMusicDTO);
        } catch (IOException e) {
            logger.error("Failed to upload file: {}", originalFilename, e);
            throw new RuntimeException("Failed to upload file: " + originalFilename, e);
        } finally {
            // Clean up temporary file
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                    logger.debug("Deleted temporary file: {}", tempFile.getAbsolutePath());
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }
}