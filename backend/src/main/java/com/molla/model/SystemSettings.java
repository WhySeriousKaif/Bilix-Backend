package com.molla.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Single-row table holding system-wide configuration for Super Admin.
 * There is always exactly one row (id = 1).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettings {

    @Id
    private Long id;

    private String systemName;
    private String systemEmail;
    private String systemPhone;

    private Integer maxStoresPerAdmin;
    private Integer sessionTimeoutMinutes;

    @Builder.Default
    private Boolean maintenanceMode = false;
    @Builder.Default
    private Boolean allowRegistration = true;
    @Builder.Default
    private Boolean emailNotifications = true;

    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    protected void onSave() {
        updatedAt = LocalDateTime.now();
    }
}
