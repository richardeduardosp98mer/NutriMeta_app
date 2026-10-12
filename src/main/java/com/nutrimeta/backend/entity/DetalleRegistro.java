package com.nutrimeta.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "detalle_registro")
public class DetalleRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long idDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_registro", nullable = false)
    private RegistroComida registro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alimento", nullable = false)
    private Alimento alimento;

    @Column(name = "cantidad_g", nullable = false, precision = 7, scale = 2)
    private BigDecimal cantidadG;

    @Column(name = "energia_kcal", nullable = false, precision = 8, scale = 2)
    private BigDecimal energiaKcal;

    @Column(name = "proteina_g", nullable = false, precision = 8, scale = 2)
    private BigDecimal proteinaG;

    @Column(name = "grasa_g", nullable = false, precision = 8, scale = 2)
    private BigDecimal grasaG;

    @Column(name = "carbohidratos_g", nullable = false, precision = 8, scale = 2)
    private BigDecimal carbohidratosG;
}
