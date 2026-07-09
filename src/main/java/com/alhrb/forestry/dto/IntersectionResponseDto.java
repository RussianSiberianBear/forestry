// IntersectionResponse.java
package com.alhrb.forestry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntersectionResponseDto {
    private boolean success;
    private String status; // "success", "warning", "error"
    private String message;
    private List<IntersectionReport> conflicts;
}