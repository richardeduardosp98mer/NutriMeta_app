package com.nutrimeta.backend.repository;

import com.nutrimeta.backend.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradorRepositorio extends JpaRepository<Administrador, Long> {

    Optional<Administrador> findByCorreo(String correo);
    long countByActivoTrue();
}
