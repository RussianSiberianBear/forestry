package com.alhrb.forestry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "user_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private Long userId;  // ← ТОЛЬКО ЭТО, никакого User!

    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> global = new HashMap<>();

    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> dashboard = new HashMap<>();

    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> profile = new HashMap<>();

    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> analytics = new HashMap<>();

    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> notifications = new HashMap<>();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}