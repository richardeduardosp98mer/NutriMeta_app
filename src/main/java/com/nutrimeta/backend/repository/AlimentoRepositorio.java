package com.nutrimeta.backend.repository;

import com.nutrimeta.backend.entity.Alimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AlimentoRepositorio extends JpaRepository<Alimento, Long> {

    @EntityGraph(attributePaths = "grupo")
    @Query("""
        SELECT a FROM Alimento a
            WHERE (:nombre IS NULL OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')))
            AND (:idGrupo IS NULL OR a.grupo.idGrupo = :idGrupo)
            AND (:activo IS NULL OR a.activo = :activo)
       """)
    Page<Alimento> buscar(
            @Param("nombre") String nombre,
            @Param("idGrupo") Integer idGrupo,
            @Param("activo") Boolean activo,
            Pageable pageable);
    boolean existsByGrupoIdGrupo(Integer idGrupo);

    // Catálogo para la app: todos (activos e inactivos) o solo los modificados después de `desde`.
    @EntityGraph(attributePaths = "grupo")
    @Query("""
        SELECT a FROM Alimento a
            WHERE (:desde IS NULL OR a.fechaActualizacion > :desde)
            ORDER BY a.idAlimento
       """)
    List<Alimento> findParaCatalogo(@Param("desde") LocalDateTime desde);

    // Marca de tiempo (reloj de la BD) del último cambio en el catálogo.
    @Query("SELECT MAX(a.fechaActualizacion) FROM Alimento a")
    LocalDateTime ultimaActualizacion();
}
