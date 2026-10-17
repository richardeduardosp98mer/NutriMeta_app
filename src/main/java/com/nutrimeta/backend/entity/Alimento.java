package com.nutrimeta.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alimento")
public class Alimento {

    public enum MetodoCarbohidratos{
        DISPONIBLES, TOTALES_MENOS_FIBRA, TOTALES, DERIVADO_ENERGIA, MANUAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alimento", columnDefinition = "int unsigned")
    private Long idAlimento;

    @Column(name = "codigo_ins", length = 8)
    private String codigoIns;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_grupo", nullable = false, columnDefinition = "tinyint unsigned")
    private GrupoAlimento grupo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "energia_kcal", nullable = false, precision = 6, scale = 1)
    private BigDecimal energiaKcal;

    @Column(name = "proteina_g", nullable = false, precision = 6, scale = 2)
    private BigDecimal proteinaG;

    @Column(name = "grasa_g", nullable = false, precision = 6, scale = 2)
    private BigDecimal grasaG;

    @Column(name = "carbohidratos_g", nullable = false, precision = 6, scale = 2)
    private BigDecimal carbohidratosG;

    @Column(name = "fibra_g", precision = 6, scale = 2)
    private BigDecimal fibraG;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_carbohidratos", nullable = false)
    private MetodoCarbohidratos metodoCarbohidratos;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false, insertable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;
}
