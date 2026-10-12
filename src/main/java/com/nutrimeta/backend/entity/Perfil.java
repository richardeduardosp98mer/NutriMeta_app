package com.nutrimeta.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "perfil")
public class Perfil {

    public enum Sexo { M, F }
    public enum Objetivo { BAJAR, MANTENER, SUBIR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_perfil")
    private Long idPerfil;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sexo sexo;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "peso_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "talla_cm", nullable = false, precision = 5, scale = 1)
    private BigDecimal tallaCm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nivel_actividad", nullable = false)
    private NivelActividad nivelActividad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Objetivo objetivo;

    @Column(name = "peso_objetivo_kg", precision = 5, scale = 2)
    private BigDecimal pesoObjetivoKg;

    @Column(name = "plazo_semanas")
    private Integer plazoSemanas;

    @Column(name = "meta_kcal", nullable = false, precision = 6, scale = 1)
    private BigDecimal metaKcal;

    @Column(name = "meta_proteina_g", nullable = false, precision = 6, scale = 1)
    private BigDecimal metaProteinaG;

    @Column(name = "meta_grasa_g", nullable = false, precision = 6, scale = 1)
    private BigDecimal metaGrasaG;

    @Column(name = "meta_carbohidratos_g", nullable = false, precision = 6, scale = 1)
    private BigDecimal metaCarbohidratosG;

    @Column(name = "meta_ajustada", nullable = false)
    private Boolean metaAjustada = false;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "fecha_actualizacion", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;
}
