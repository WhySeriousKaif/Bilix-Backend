package com.molla.service.impl;

import com.molla.model.SystemSettings;
import com.molla.model.User;
import com.molla.payload.dto.SystemSettingsDto;
import com.molla.repository.SystemSettingsRepository;
import com.molla.service.SystemSettingsService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemSettingsServiceImpl implements SystemSettingsService {

    private static final Long SINGLETON_ID = 1L;

    private final SystemSettingsRepository systemSettingsRepository;
    private final UserService userService;
    private final AccessControlService accessControlService;

    @Override
    public SystemSettingsDto getSettings() {
        accessControlService.requireAdmin(userService.getCurrentUser());
        return toDto(getOrCreateSettings());
    }

    @Override
    public SystemSettingsDto updateSettings(SystemSettingsDto dto) {
        accessControlService.requireAdmin(userService.getCurrentUser());
        SystemSettings settings = getOrCreateSettings();

        settings.setSystemName(dto.getSystemName());
        settings.setSystemEmail(dto.getSystemEmail());
        settings.setSystemPhone(dto.getSystemPhone());
        settings.setMaxStoresPerAdmin(dto.getMaxStoresPerAdmin());
        settings.setSessionTimeoutMinutes(dto.getSessionTimeoutMinutes());
        if (dto.getMaintenanceMode() != null) settings.setMaintenanceMode(dto.getMaintenanceMode());
        if (dto.getAllowRegistration() != null) settings.setAllowRegistration(dto.getAllowRegistration());
        if (dto.getEmailNotifications() != null) settings.setEmailNotifications(dto.getEmailNotifications());

        return toDto(systemSettingsRepository.save(settings));
    }

    private SystemSettings getOrCreateSettings() {
        return systemSettingsRepository.findById(SINGLETON_ID).orElseGet(() -> {
            User currentUser = userService.getCurrentUser();
            SystemSettings defaults = SystemSettings.builder()
                    .id(SINGLETON_ID)
                    .systemName("Bilix POS")
                    .systemEmail(currentUser.getEmail())
                    .systemPhone(currentUser.getPhone())
                    .maxStoresPerAdmin(1)
                    .sessionTimeoutMinutes(30)
                    .maintenanceMode(false)
                    .allowRegistration(true)
                    .emailNotifications(true)
                    .build();
            return systemSettingsRepository.save(defaults);
        });
    }

    private SystemSettingsDto toDto(SystemSettings settings) {
        SystemSettingsDto dto = new SystemSettingsDto();
        dto.setSystemName(settings.getSystemName());
        dto.setSystemEmail(settings.getSystemEmail());
        dto.setSystemPhone(settings.getSystemPhone());
        dto.setMaxStoresPerAdmin(settings.getMaxStoresPerAdmin());
        dto.setSessionTimeoutMinutes(settings.getSessionTimeoutMinutes());
        dto.setMaintenanceMode(settings.getMaintenanceMode());
        dto.setAllowRegistration(settings.getAllowRegistration());
        dto.setEmailNotifications(settings.getEmailNotifications());
        return dto;
    }
}
