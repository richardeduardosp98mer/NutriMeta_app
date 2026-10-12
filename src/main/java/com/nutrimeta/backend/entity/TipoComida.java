package com.nutrimeta.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tipo_comida")
public class TipoComida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_comida")
    private Integer idTipoComida;
    @Column(nullable = false, length = 30)
    private String nombre;
    @Column(nullable = false)
    private Integer orden = 0;
}
