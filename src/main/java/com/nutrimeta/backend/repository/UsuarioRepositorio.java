package com.nutrimeta.backend.repository;

import com.nutrimeta.backend.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByFirebaseUid(String firebaseUid);

    @Query("""
        SELECT u FROM Usuario u
            WHERE :texto IS NULL
                OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :texto, '%'))     
    """)
    Page<Usuario> buscar(
        @Param("texto") String texto, Pageable pageable
    );

}
