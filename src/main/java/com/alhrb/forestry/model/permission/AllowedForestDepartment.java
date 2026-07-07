package com.alhrb.forestry.model.permission;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "allowed_forest_departments", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(AllowedForestDepartmentId.class)
public class AllowedForestDepartment {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "forestry_unit_id", nullable = false)
    private Long forestryUnitId;
}