package com.alhrb.forestry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "municipal_district")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MunicipalDistrict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @OneToMany(mappedBy = "municipalDistrict", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Forestry> forestries = new ArrayList<>();

    @Override
    public String toString() {
        return name != null ? name : "Без названия";
    }
}
