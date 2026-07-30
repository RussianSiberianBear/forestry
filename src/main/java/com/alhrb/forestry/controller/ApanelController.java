package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.UserDto;
import com.alhrb.forestry.dto.abgrid.GridP;
import com.alhrb.forestry.dto.abgrid.GridRequest;
import com.alhrb.forestry.files.StoredFileService;
import com.alhrb.forestry.user.UserMapper;
import com.alhrb.forestry.user.service.UserService;
import com.alhrb.forestry.util.DateTimeUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
public class ApanelController {

    private final UserService userService;
    private final UserMapper mapper;
    private final StoredFileService fileService;

    public ApanelController(UserService userService, UserMapper mapper, StoredFileService fileService) {
        this.userService = userService;
        this.mapper = mapper;
        this.fileService = fileService;
    }

    @GetMapping("/apanel")
    public String apanel(Model model) {

        return "admin/apanel";
    }

    @PostMapping("/api/apanel/users")
    public ResponseEntity<?> userOperation(@RequestBody(required = false) GridRequest req) {

        try {

            GridP p = (req != null) ? req.first() : null;

            if (p.getOper().equalsIgnoreCase("read")) {
                Map<String, Object> filter = p.getFilter();

                if (!DateTimeUtil.isValidatePeriod("dateUserRegFrom", "dateUserRegTo", filter)) {
                    return ResponseEntity.ok(Map.of("success", false, "message", "Дата начала регистрации превышает дату окончания!"));
                }
                if (!DateTimeUtil.isValidatePeriod("datePurchaseLicenseFrom", "datePurchaseLicenseTo", filter)) {
                    return ResponseEntity.ok(Map.of("success", false, "message", "Дата начала покупки превышает дату окончания!"));
                }
                if (!DateTimeUtil.isValidatePeriod("dateUserLicenseFrom", "dateUserLicenseTo", filter)) {
                    return ResponseEntity.ok(Map.of("success", false, "message", "Дата начала действия превышает дату окончания!"));
                }
                if (!DateTimeUtil.isValidatePeriod("userPurchasingUpdatesDateFrom", "userPurchasingUpdatesDateTo", filter)) {
                    return ResponseEntity.ok(Map.of("success", false, "message", "Дата начала периода обновления превышает дату окончания!"));
                }

                return ResponseEntity.ok(userService.findAll(p));
            }

            if (p.getOper().equalsIgnoreCase("create")) {
                Map<String, Object> data = new HashMap<>();
                List<UserDto> rows = new ArrayList<>();
                Map<String, Object> res;
                res = userService.createUser(p);
                if (Boolean.TRUE.equals(res.get("success"))) {
                    var dataRows = (Map<Long, Object>) res.get("rows");
                    for (var dataRow : dataRows.entrySet()) {
                        var user = userService.findById(dataRow.getKey()).orElseThrow(null);
                        var clientId = dataRow.getValue();
                        user.set__clientId(clientId != null ? clientId.toString() : null);
                        rows.add(mapper.toDto(user));
                    }

                    data.put("rows", rows); // ВАЖНО: просто rows, НЕ List.of(rows)
                }
                res.remove("rows");
                if (!rows.isEmpty()) data.put("rows", rows);
                data.put("opId", p.getOpId());
                res.put("data", data);
                return ResponseEntity.ok(res);
            }

            if (p.getOper().equalsIgnoreCase("update")) {
                Map<String, Object> data = new HashMap<>();
                List<UserDto> rows = new ArrayList<>();
                Map<String, Object> res;
                res = userService.updateUser(p);
                if (Boolean.TRUE.equals(res.get("success"))) {
                    var dataRows = (Map<Long, Object>) res.get("rows");
                    for (var dataRow : dataRows.entrySet()) {
                        var user = userService.findById(dataRow.getKey()).orElseThrow(null);
                        rows.add(mapper.toDto(user));
                    }
                }
                res.remove("rows");
                if (!rows.isEmpty()) data.put("rows", rows);
                data.put("opId", p.getOpId());
                res.put("data", data);
                return ResponseEntity.ok(res);
            }

            if (p.getOper().equalsIgnoreCase("delete")) {
                return ResponseEntity.ok(userService.deleteUser(p));
            }

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Ошибка при выполнении операции!"));
        }

        return ResponseEntity.ok(Map.of("success", false, "message", "Неподдерживаемая операция!"));
    }

    @PostMapping("/api/apanel/userfiles")
    public ResponseEntity<?> userFiles(@RequestBody(required = false) GridRequest req) {
        try {
            GridP p = (req != null) ? req.first() : null;

            if (p.getOper().equalsIgnoreCase("read")) {
                return ResponseEntity.ok(fileService.getUserFiles(p));
            }


        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Ошибка при выполнении операции!"));
        }

        return ResponseEntity.ok(Map.of("success", false, "message", "Неподдерживаемая операция!"));
    }
}
