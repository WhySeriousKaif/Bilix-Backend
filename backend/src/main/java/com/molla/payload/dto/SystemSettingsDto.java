package com.molla.payload.dto;

import lombok.Data;

@Data
public class SystemSettingsDto {
    private String systemName;
    private String systemEmail;
    private String systemPhone;
    private Integer maxStoresPerAdmin;
    private Integer sessionTimeoutMinutes;
    private Boolean maintenanceMode;
    private Boolean allowRegistration;
    private Boolean emailNotifications;
}
