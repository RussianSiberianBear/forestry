package com.alhrb.forestry.service;

import com.alhrb.forestry.model.CuttingArea;
import com.alhrb.forestry.model.ForestStand;
import com.alhrb.forestry.repository.ForestStandRepository;
import com.alhrb.forestry.repository.ForestryUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForestStandService {

    private final ForestStandRepository forestStandRepository;
    private final ForestryUnitRepository forestryUnitRepository;
    @Value("${forest.validation.min-area:0.01}")
    private double minArea;

    @Transactional
    public ForestStand save(ForestStand forestStand) {
        return forestStandRepository.save(forestStand);
    }

    public List<ForestStand> findAll() {
        return forestStandRepository.findAll();
    }

}
