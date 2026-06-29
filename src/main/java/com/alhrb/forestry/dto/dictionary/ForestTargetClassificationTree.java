package com.alhrb.forestry.dto.dictionary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForestTargetClassificationTree {

    private Long id;
    private String name;
    private String fullName;
    private String code;
    private Short level;
    @Builder.Default
    private List<ForestTargetClassificationTree> children = new ArrayList<>();
}