package com.nutrimeta.backend.repository;

import com.nutrimeta.backend.entity.RegistroComida;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface RegistroComidaRepositorio extends JpaRepository<RegistroComida, Long> {

    @Query("""
        SELECT r FROM RgistrarComida r
            WHERE r.eliminado = false
                AND (:isUsuario IN NULL OR r.usuario.idUsuario = :idUsuario)
                AND (:desde IS NULL OR r.fecha >= :desde) 
                AND (:hasta IS NULL OR r.fecha >= :hasta)
               ORDER BY r.fecha DESC
    """)
    Page<RegistroComida> buscar(
            @Param("idUsuario") Long idUsuario,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
            );
    boolean existsByTipoComidaIdTipoComida(Integer idTipoComida);
}
