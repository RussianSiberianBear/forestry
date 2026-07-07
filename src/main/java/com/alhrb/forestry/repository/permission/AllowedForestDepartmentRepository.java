package com.alhrb.forestry.repository.permission;

import com.alhrb.forestry.model.permission.AllowedForestDepartment;
import com.alhrb.forestry.model.permission.AllowedForestDepartmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AllowedForestDepartmentRepository
        extends JpaRepository<AllowedForestDepartment, AllowedForestDepartmentId> {

    // Можно добавить дополнительные методы запросов
    void deleteByUserId(Long userId);
    void deleteByForestryUnitId(Long forestryUnitId);
}