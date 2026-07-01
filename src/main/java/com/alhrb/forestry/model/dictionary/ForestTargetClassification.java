package com.alhrb.forestry.model.dictionary;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "forest_target_classification")
public class ForestTargetClassification {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ForestTargetClassification parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ForestTargetClassification> children = new ArrayList<>();

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "full_name", columnDefinition = "TEXT")
    private String fullName;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "\"level\"", nullable = false)
    private Short level;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Вспомогательные методы для работы с иерархией
    public void addChild(ForestTargetClassification child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(ForestTargetClassification child) {
        children.remove(child);
        child.setParent(null);
    }
}
