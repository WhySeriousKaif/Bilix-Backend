package com.molla.controllers;

import com.molla.payload.dto.SystemSettingsDto;
import com.molla.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingsController {

    private final SystemSettingsService systemSettingsService;

    @GetMapping
    public ResponseEntity<SystemSettingsDto> getSettings() {
        return ResponseEntity.ok(systemSettingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<SystemSettingsDto> updateSettings(@RequestBody SystemSettingsDto dto) {
        return ResponseEntity.ok(systemSettingsService.updateSettings(dto));
    }
}
