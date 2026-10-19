package com.nutrimeta.backend.repository;

import com.nutrimeta.backend.entity.NivelActividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NivelActividadRepositorio extends JpaRepository<NivelActividad, Integer> {

    // Para la sincronizacion: el perfil del celular trae el codigo del nivel (MODERADO, ...).
    Optional<NivelActividad> findByCodigo(String codigo);
}
