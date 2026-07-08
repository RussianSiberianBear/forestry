package com.alhrb.forestry.service;

import com.alhrb.forestry.repository.ForestryUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForestStandService {

    private final ForestryUnitRepository forestryUnitRepository;

}
