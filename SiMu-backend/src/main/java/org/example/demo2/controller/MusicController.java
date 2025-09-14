package org.example.demo2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.demo2.dto.ErrorResponse;
import org.example.demo2.dto.MusicDTO;
import org.example.demo2.dto.MusicUploadDTO;
import org.example.demo2.exception.FileProcessingException;
import org.example.demo2.exception.MusicNotFoundException;
import org.example.demo2.mapper.MusicMapper;
import org.example.demo2.services.MusicService;
import org.example.demo2.services.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing music-related operations.
 * Provides endpoints for retrieving and uploading music files.
 */
@Tag(name = "Music", description = "API for managing music records and file uploads")
@RestController
@RequestMapping("/api/v1/music")
@Validated
@SecurityRequirement(name = "bearerAuth")
public class MusicController {

    private static final Logger logger = LoggerFactory.getLogger(MusicController.class);

    private final MusicService musicService;
    private final S3Service s3Service;
    private final MusicMapper musicMapper;

    @Autowired
    public MusicController(MusicService musicService, S3Service s3Service, MusicMapper musicMapper) {
        this.musicService = musicService;
        this.s3Service = s3Service;
        this.musicMapper = musicMapper;
    }

    /**
     * Retrieves a music record by ID.
     *
     * @param id The ID of the music record.
     * @return Music record information.
     */
    @Operation(summary = "Get music by ID", description = "Retrieves a music record by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Music record retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MusicDTO.class))),
            @ApiResponse(responseCode = "404", description = "Music record not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MusicDTO> getMusic(
            @Parameter(description = "ID of the music record", required = true)
            @PathVariable @Min(1) Long id) {
        logger.info("Fetching music with ID: {}", id);
        MusicDTO musicDTO = musicService.getMusic(id)
                .orElseThrow(() -> new MusicNotFoundException("Music record not found with ID: " + id));
        logger.debug("Retrieved music: {}", musicDTO);
        return ResponseEntity.ok(musicDTO);
    }

    /**
     * Retrieves all music records.
     *
     * @return List of music records.
     */
    @Operation(summary = "Get all music records", description = "Returns a list of all music records")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Music records retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MusicDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<MusicDTO>> getAllMusic() {
        logger.info("Fetching all music records");
        List<MusicDTO> musicList = musicService.getAllMusic();
        logger.debug("Retrieved {} music records", musicList.size());
        return ResponseEntity.ok(musicList);
    }

    /**
     * Uploads a new music file to S3 and saves its metadata.
     *
     * @param file           The music file to upload.
     * @param musicUploadDTO The music metadata.
     * @param uriBuilder     URI builder for location header.
     * @return Created music record information.
     */
    @Operation(summary = "Upload music file", description = "Uploads a music file to S3 and saves its metadata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Music file uploaded successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MusicDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or metadata",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MusicDTO> uploadMusic(
            @Parameter(description = "Music file to upload", required = true)
            @RequestPart("file") @NotNull MultipartFile file,
            @Parameter(description = "Music metadata", required = true)
            @RequestPart("music") @Valid MusicUploadDTO musicUploadDTO,
            UriComponentsBuilder uriBuilder) {
        logger.info("Received file upload request: filename={}", file.getOriginalFilename());

        if (file.isEmpty()) {
            logger.warn("Uploaded file is empty");
            throw new FileProcessingException("Uploaded file is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            logger.warn("File name is empty");
            throw new FileProcessingException("File name must not be empty");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        String s3Key = String.format("%s_%s_%s.%s",
                musicUploadDTO.name(),
                musicUploadDTO.artist(),
                UUID.randomUUID().toString(),
                extension);

        File tempFile = null;
        try {
            // Create temporary file
            tempFile = File.createTempFile("music_", "." + extension);
            file.transferTo(tempFile);

            // Upload to S3
            String fileUrl = s3Service.uploadFile(s3Key, tempFile);

            // Create MusicDTO with fileUrl
            MusicDTO musicToSave = new MusicDTO(null, musicUploadDTO.name(), musicUploadDTO.artist(), fileUrl);
            MusicDTO savedMusicDTO = musicService.saveMusic(musicToSave);

            URI location = uriBuilder.path("/api/v1/music/{id}")
                    .buildAndExpand(savedMusicDTO.id())
                    .toUri();
            logger.info("Music uploaded and saved: id={}, url={}", savedMusicDTO.id(), fileUrl);

            return ResponseEntity.created(location).body(savedMusicDTO);
        } catch (IOException e) {
            logger.error("Failed to upload file: {}", originalFilename, e);
            throw new FileProcessingException("Failed to upload file: " + originalFilename, e);
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

    /**
     * Handles MusicNotFoundException.
     */
    @ExceptionHandler(MusicNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMusicNotFound(MusicNotFoundException ex) {
        logger.error("Music error: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), "MUSIC_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles FileProcessingException.
     */
    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ErrorResponse> handleFileProcessingException(FileProcessingException ex) {
        logger.error("File processing error: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), "FILE_PROCESSING_ERROR");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}