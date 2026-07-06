package com.alhrb.forestry.model.permission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllowedForestDepartmentId implements Serializable {

    private Long userId;
    private Long forestryUnitId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllowedForestDepartmentId that = (AllowedForestDepartmentId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(forestryUnitId, that.forestryUnitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, forestryUnitId);
    }
}