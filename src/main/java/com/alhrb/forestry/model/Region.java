package com.alhrb.forestry.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "region")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "code", length = 10)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @JsonIgnore  // ← ДОБАВИТЬ!
    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MunicipalDistrict> municipalDistricts = new ArrayList<>();

    @Override
    public String toString() {
        return name != null ? name : "Без названия";
    }
}
