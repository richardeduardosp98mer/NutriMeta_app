package com.nutrimeta.backend.repository;

import com.nutrimeta.backend.entity.Perfil;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilRepositorio extends JpaRepository<Perfil, Long> {

    @EntityGraph(attributePaths = "nivelActividad")
    Optional<Perfil> findByUsuarioIdUsuario(Long idUsuario);
    boolean existsByNivelActividadIdNivelActividad(Integer idNivelActividad);
}
