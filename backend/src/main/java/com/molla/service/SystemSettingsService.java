package com.molla.service;

import com.molla.payload.dto.SystemSettingsDto;

public interface SystemSettingsService {
    SystemSettingsDto getSettings();
    SystemSettingsDto updateSettings(SystemSettingsDto dto);
}
