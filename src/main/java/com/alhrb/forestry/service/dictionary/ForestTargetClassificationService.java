package com.alhrb.forestry.service.dictionary;

import com.alhrb.forestry.dto.dictionary.ForestTargetClassificationRequest;
import com.alhrb.forestry.dto.dictionary.ForestTargetClassificationResponse;
import com.alhrb.forestry.dto.dictionary.ForestTargetClassificationTree;
import com.alhrb.forestry.mapper.dictionary.ForestTargetClassificationMapper;
import com.alhrb.forestry.model.dictionary.ForestTargetClassification;
import com.alhrb.forestry.repository.dictionary.ForestTargetClassificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForestTargetClassificationService {

    private final ForestTargetClassificationRepository repository;
    private final ForestTargetClassificationMapper mapper;

    @Transactional
    public ForestTargetClassificationResponse create(ForestTargetClassificationRequest request) {
        log.debug("Creating new classification: {}", request.getName());

        validateParent(request.getParentId());
        validateUniqueName(request.getParentId(), request.getName());

        ForestTargetClassification entity = mapper.toEntity(request);

        if (request.getParentId() != null) {
            ForestTargetClassification parent = repository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent not found with id: " + request.getParentId()));
            entity.setParent(parent);
        }

        ForestTargetClassification saved = repository.save(entity);
        log.info("Created classification with id: {}", saved.getId());

        return mapper.toResponse(saved);
    }

    @Transactional
    public ForestTargetClassificationResponse update(Long id, ForestTargetClassificationRequest request) {
        log.debug("Updating classification with id: {}", id);

        ForestTargetClassification existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Classification not found with id: " + id));

        if (request.getParentId() != null && request.getParentId().equals(id)) {
            throw new RuntimeException("Cannot set parent to itself");
        }

        if (!existing.getName().equals(request.getName()) ||
                (existing.getParent() != null && !existing.getParent().getId().equals(request.getParentId())) ||
                (existing.getParent() == null && request.getParentId() != null)) {
            validateUniqueName(request.getParentId(), request.getName());
        }

        mapper.updateEntity(request, existing);

        if (request.getParentId() != null) {
            ForestTargetClassification parent = repository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent not found with id: " + request.getParentId()));
            existing.setParent(parent);
        } else {
            existing.setParent(null);
        }

        ForestTargetClassification updated = repository.save(existing);
        log.info("Updated classification with id: {}", updated.getId());

        return mapper.toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        log.debug("Deleting classification with id: {}", id);

        if (!repository.existsById(id)) {
            throw new RuntimeException("Classification not found with id: " + id);
        }

        repository.deleteById(id);
        log.info("Deleted classification with id: {}", id);
    }

    public ForestTargetClassificationResponse getById(Long id) {
        log.debug("Fetching classification with id: {}", id);

        ForestTargetClassification entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Classification not found with id: " + id));

        return mapper.toResponse(entity);
    }

    public List<ForestTargetClassificationResponse> getAll() {
        log.debug("Fetching all classifications");
        return mapper.toResponseList(repository.findAll());
    }

    public List<ForestTargetClassificationResponse> getRootNodes() {
        log.debug("Fetching root nodes");
        return mapper.toResponseList(repository.findByParentIsNull());
    }

    public List<ForestTargetClassificationTree> getTree() {
        log.debug("Fetching full tree");
        List<ForestTargetClassification> roots = repository.findByParentIsNull();
        return mapper.toTreeList(roots);
    }

    public List<ForestTargetClassificationResponse> getByLevel(Short level) {
        log.debug("Fetching classifications by level: {}", level);
        return mapper.toResponseList(repository.findByLevel(level));
    }

    public List<ForestTargetClassificationResponse> getChildren(Long parentId) {
        log.debug("Fetching children for parent id: {}", parentId);

        if (!repository.existsById(parentId)) {
            throw new RuntimeException("Parent not found with id: " + parentId);
        }

        return mapper.toResponseList(repository.findByParentId(parentId));
    }

    public List<ForestTargetClassificationResponse> getAllDescendants(Long parentId) {
        log.debug("Fetching all descendants for parent id: {}", parentId);

        if (!repository.existsById(parentId)) {
            throw new RuntimeException("Parent not found with id: " + parentId);
        }

        List<ForestTargetClassification> descendants = repository.findAllDescendants(parentId);
        return mapper.toResponseList(descendants);
    }

    public long countChildren(Long parentId) {
        log.debug("Counting children for parent id: {}", parentId);
        return repository.countByParentId(parentId);
    }

    private void validateParent(Long parentId) {
        if (parentId != null && !repository.existsById(parentId)) {
            throw new RuntimeException("Parent not found with id: " + parentId);
        }
    }

    private void validateUniqueName(Long parentId, String name) {
        if (repository.existsByParentIdAndName(parentId, name)) {
            throw new RuntimeException(
                    String.format("Classification with name '%s' already exists under parent id: %s",
                            name, parentId)
            );
        }
    }
}
