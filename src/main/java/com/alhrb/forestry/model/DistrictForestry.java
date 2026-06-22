package com.alhrb.forestry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "district_forestry")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistrictForestry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name; // Например: "Пригородное участковое лесничество"

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forestry_id")
    private Forestry forestry;

    @OneToMany(mappedBy = "districtForestry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Quarter> quarters = new ArrayList<>();
}
