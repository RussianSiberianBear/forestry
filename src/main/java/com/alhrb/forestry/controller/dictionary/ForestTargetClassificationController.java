package com.alhrb.forestry.controller.dictionary;

import com.alhrb.forestry.dto.dictionary.ForestTargetClassificationRequest;
import com.alhrb.forestry.dto.dictionary.ForestTargetClassificationResponse;
import com.alhrb.forestry.dto.dictionary.ForestTargetClassificationTree;
import com.alhrb.forestry.service.dictionary.ForestTargetClassificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forest-classifications")
@RequiredArgsConstructor
public class ForestTargetClassificationController {

    private final ForestTargetClassificationService service;

    @PostMapping
    public ResponseEntity<ForestTargetClassificationResponse> create(
            @Valid @RequestBody ForestTargetClassificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ForestTargetClassificationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ForestTargetClassificationRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForestTargetClassificationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ForestTargetClassificationResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/roots")
    public ResponseEntity<List<ForestTargetClassificationResponse>> getRoots() {
        return ResponseEntity.ok(service.getRootNodes());
    }

    @GetMapping("/tree")
    public ResponseEntity<List<ForestTargetClassificationTree>> getTree() {
        return ResponseEntity.ok(service.getTree());
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<ForestTargetClassificationResponse>> getByLevel(@PathVariable Short level) {
        return ResponseEntity.ok(service.getByLevel(level));
    }

    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<ForestTargetClassificationResponse>> getChildren(@PathVariable Long parentId) {
        return ResponseEntity.ok(service.getChildren(parentId));
    }

    @GetMapping("/{parentId}/descendants")
    public ResponseEntity<List<ForestTargetClassificationResponse>> getDescendants(@PathVariable Long parentId) {
        return ResponseEntity.ok(service.getAllDescendants(parentId));
    }

    @GetMapping("/{parentId}/children/count")
    public ResponseEntity<Long> countChildren(@PathVariable Long parentId) {
        return ResponseEntity.ok(service.countChildren(parentId));
    }
}