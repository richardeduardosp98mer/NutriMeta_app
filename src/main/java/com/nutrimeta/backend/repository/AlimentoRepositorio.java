package com.nutrimeta.backend.repository;

import com.nutrimeta.backend.entity.Alimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlimentoRepositorio extends JpaRepository<Alimento, Long> {

    @Query("""
        SELECT a FROM Alimento a
            WHERE (:nombre IS NULL OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')))
            AND (:idGrupo IS NULL OR a.grupo.idGrupo = :idGrupo)
            AND (:activo IS NULL OR a.activo = :activo)
       """)
    Page<Alimento> buscar(
            @Param("nomnre") String nombre,
            @Param("idGrupo") Integer idGrupo,
            @Param("activo") Boolean activo,
            Pageable pageable);
    boolean existsByGrupoIdGrupo(Integer idGrupo);
}
