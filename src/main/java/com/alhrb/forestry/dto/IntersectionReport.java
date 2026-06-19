package com.alhrb.forestry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntersectionReport {
    private Long plot1Id;
    private String plot1Number;
    private Long plot2Id;
    private String plot2Number;
    private Double overlapArea;
    private String severity; // "CRITICAL", "WARNING", "OK"
}
