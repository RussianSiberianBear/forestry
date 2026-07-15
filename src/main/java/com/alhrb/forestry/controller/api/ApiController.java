package com.alhrb.forestry.controller.api;

import com.alhrb.forestry.dto.abgrid.GridP;
import com.alhrb.forestry.dto.abgrid.GridRequest;
import com.alhrb.forestry.service.ForestryUnitService;
import com.alhrb.forestry.util.SecurityHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@PreAuthorize("isAuthenticated()")
public class ApiController {

    private final ForestryUnitService forestryUnitService;
    private final SecurityHelper securityHelper;

    public ApiController(ForestryUnitService forestryUnitService, SecurityHelper securityHelper) {
        this.forestryUnitService = forestryUnitService;
        this.securityHelper = securityHelper;
    }

    @PostMapping("/api/common/appointedForesty")
    public ResponseEntity<?> getAppointedForestry(@RequestBody(required = false) GridRequest req) {

        try {
            GridP p = (req != null) ? req.first() : null;
            if (p == null || p.getOper() == null) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message", "Некорректный запрос"
                        )
                );
            }
            Long userId = securityHelper.getCurrentUserId();

            if (p.getOper().equalsIgnoreCase("read")) {
                return ResponseEntity.ok(forestryUnitService.findAllowedForestries(userId, p));
            }

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Ошибка при выполнении операции!"));
        }


        return ResponseEntity.ok(Map.of("success", false, "message", "Неподдерживаемая операция!"));
    }


}
