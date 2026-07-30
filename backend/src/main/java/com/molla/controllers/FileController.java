package com.molla.controllers;

import com.molla.payload.response.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Basic file upload endpoint to satisfy "file upload" requirement.
 * Stores files under ./uploads and returns a relative URL/path.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Path UPLOAD_DIR = Paths.get("uploads");

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "File is empty"
            ));
        }

        if (!Files.exists(UPLOAD_DIR)) {
            Files.createDirectories(UPLOAD_DIR);
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex != -1) {
            extension = originalFilename.substring(dotIndex);
        }
        String storedName = UUID.randomUUID() + extension;

        Path targetPath = UPLOAD_DIR.resolve(storedName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "File uploaded successfully");
        body.put("fileName", storedName);
        body.put("relativePath", "/uploads/" + storedName);

        return ResponseEntity.ok(body);
    }
}

