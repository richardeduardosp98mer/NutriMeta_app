package com.nutrimeta.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "grupo_alimento")
public class GrupoAlimento {
    // ID del alimento
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grupo", columnDefinition = "tinyint unsigned")
    private Integer idGrupo;
    // Letra del grupo en las Tablas del INS
    @Column(nullable = false, length = 1, columnDefinition = "char(1)")
    private String codigo;
    // Nombre del alimento
    @Column(nullable = false, length = 60)
    private String nombre;
}
