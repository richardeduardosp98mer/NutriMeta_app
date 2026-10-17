package com.nutrimeta.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "nivel_actividad")
public class NivelActividad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nivel_actividad", columnDefinition = "tinyint unsigned")
    private Integer idNivelActividad;
    @Column(nullable = false, length = 20)
    private String codigo;
    @Column(nullable = false, length = 40)
    private String nombre;
    @Column(nullable = false, length = 150)
    private String descripcion;
    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal factor;
    @Column(nullable = false, columnDefinition = "tinyint unsigned")
    private Integer orden = 0;



}
