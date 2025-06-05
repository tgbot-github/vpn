package com.hivpn.server.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Controller
@RequestMapping("/download")
public class DownloadController {

    @GetMapping("/apk")
    public ResponseEntity<Resource> downloadApk() {
        try {
            Path filePath = Paths.get("hivpn.apk");
            log.info("Attempting to download APK from path: {}", filePath.toAbsolutePath());
            
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("APK file found and readable");
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"hivpn.apk\"")
                        .body(resource);
            } else {
                log.error("APK file not found or not readable at path: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            log.error("Error while downloading APK file", e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 
