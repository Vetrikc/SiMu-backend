package org.example.simu.music.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.simu.music.dto.ErrorResponse;
import org.example.simu.music.dto.MusicDTO;
import org.example.simu.music.dto.MusicUploadDTO;
import org.example.simu.music.exception.FileProcessingException;
import org.example.simu.music.exception.MusicNotFoundException;
import org.example.simu.music.service.MusicService;
import org.example.simu.music.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

/**
 * Public contract for {@code /api/v1/music}. Unchanged behaviour: the file goes to S3 first and
 * only then the metadata row is written.
 */
@Tag(name = "Music", description = "API for managing music records and file uploads")
@RestController
@RequestMapping("/api/v1/music")
@Validated
@SecurityRequirement(name = "bearerAuth")
public class MusicController {

    private static final Logger log = LoggerFactory.getLogger(MusicController.class);

    private final MusicService musicService;
    private final S3Service s3Service;

    public MusicController(MusicService musicService, S3Service s3Service) {
        this.musicService = musicService;
        this.s3Service = s3Service;
    }

    @Operation(summary = "Get music by ID", description = "Retrieves a music record by its ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MusicDTO> getMusic(@PathVariable Long id) {
        log.info("Fetching music with ID: {}", id);
        return ResponseEntity.ok(musicService.getMusic(id));
    }

    @Operation(summary = "Get all music records", description = "Returns a list of all music records")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<MusicDTO>> getAllMusic() {
        log.info("Fetching all music records");
        return ResponseEntity.ok(musicService.getAllMusic());
    }

    @Operation(summary = "Upload music file", description = "Uploads a music file to S3 and saves its metadata")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MusicDTO> uploadMusic(@RequestPart("file") MultipartFile file,
                                                @RequestPart("music") @Valid MusicUploadDTO musicUploadDTO,
                                                UriComponentsBuilder uriBuilder) {
        log.info("Received file upload request: filename={}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new FileProcessingException("Uploaded file is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new FileProcessingException("File name must not be empty");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        String s3Key = String.format("%s_%s_%s.%s",
                musicUploadDTO.name(), musicUploadDTO.artist(), UUID.randomUUID(), extension);

        File tempFile = null;
        try {
            tempFile = File.createTempFile("music_", "." + extension);
            file.transferTo(tempFile);

            String fileUrl = s3Service.uploadFile(s3Key, tempFile);
            MusicDTO saved = musicService.saveMusic(
                    new MusicDTO(null, musicUploadDTO.name(), musicUploadDTO.artist(), fileUrl));

            URI location = uriBuilder.path("/api/v1/music/{id}").buildAndExpand(saved.id()).toUri();
            log.info("Music uploaded and saved: id={}, url={}", saved.id(), fileUrl);
            return ResponseEntity.created(location).body(saved);
        } catch (IOException e) {
            log.error("Failed to upload file: {}", originalFilename, e);
            throw new FileProcessingException("Failed to upload file: " + originalFilename, e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }

    @ExceptionHandler(MusicNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMusicNotFound(MusicNotFoundException ex) {
        log.warn("Music error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), "MUSIC_NOT_FOUND"));
    }

    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ErrorResponse> handleFileProcessingException(FileProcessingException ex) {
        log.warn("File processing error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), "FILE_PROCESSING_ERROR"));
    }
}
