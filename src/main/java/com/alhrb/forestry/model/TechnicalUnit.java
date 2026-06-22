package com.alhrb.forestry.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "technical_unit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_main")
    private Boolean isMain = false;

    // ===== СВЯЗЬ С УЧАСТКОВЫМ ЛЕСНИЧЕСТВОМ =====
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_forestry_id", nullable = false)  // ← nullable = false
    private DistrictForestry districtForestry;

    @JsonIgnore
    @OneToMany(mappedBy = "technicalUnit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Quarter> quarters = new ArrayList<>();

    @Override
    public String toString() {
        return name != null ? name : "Без названия";
    }
}
