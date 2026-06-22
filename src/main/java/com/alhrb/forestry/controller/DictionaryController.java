package com.alhrb.forestry.controller;

import com.alhrb.forestry.model.*;
import com.alhrb.forestry.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DictionaryController {

    private final RegionService regionService;
    private final MunicipalDistrictService municipalDistrictService;
    private final ForestryService forestryService;
    private final DistrictForestryService districtForestryService;
    private final QuarterService quarterService;

    @GetMapping("/regions")
    public ResponseEntity<List<Region>> getRegions() {
        return ResponseEntity.ok(regionService.findAll());
    }

    @GetMapping("/municipal-districts/by-region/{regionId}")
    public ResponseEntity<List<MunicipalDistrict>> getMunicipalDistricts(@PathVariable Long regionId) {
        return ResponseEntity.ok(municipalDistrictService.findByRegionId(regionId));
    }

    @GetMapping("/forestries/by-district/{municipalDistrictId}")
    public ResponseEntity<List<Forestry>> getForestries(@PathVariable Long municipalDistrictId) {
        return ResponseEntity.ok(forestryService.findByMunicipalDistrictId(municipalDistrictId));
    }

    @GetMapping("/district-forestries/by-forestry/{forestryId}")
    public ResponseEntity<List<DistrictForestry>> getDistrictForestries(@PathVariable Long forestryId) {
        return ResponseEntity.ok(districtForestryService.findByForestryId(forestryId));
    }

    @GetMapping("/quarters/by-district/{districtForestryId}")
    public ResponseEntity<List<Quarter>> getQuarters(@PathVariable Long districtForestryId) {
        return ResponseEntity.ok(quarterService.findByDistrictForestry(districtForestryId));
    }
}
