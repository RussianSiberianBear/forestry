package com.alhrb.forestry.dto.dictionary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForestTargetClassificationResponse {

    private Long id;
    private Long parentId;
    private String parentName;
    private String name;
    private String fullName;
    private String code;
    private Short level;
    private LocalDateTime createdAt;
    private List<ForestTargetClassificationResponse> children;
}