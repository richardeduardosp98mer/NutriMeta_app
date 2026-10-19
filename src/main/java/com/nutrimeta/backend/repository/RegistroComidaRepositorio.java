package com.nutrimeta.backend.repository;

import com.nutrimeta.backend.entity.RegistroComida;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RegistroComidaRepositorio extends JpaRepository<RegistroComida, Long> {

    @EntityGraph(attributePaths = {"usuario", "tipoComida"})
    @Query("""
        SELECT r FROM RegistroComida r
            WHERE r.eliminado = false
                AND (:idUsuario IS NULL OR r.usuario.idUsuario = :idUsuario)
                AND (:desde IS NULL OR r.fecha >= :desde) 
                AND (:hasta IS NULL OR r.fecha <= :hasta)
               ORDER BY r.fecha DESC
    """)
    Page<RegistroComida> buscar(
            @Param("idUsuario") Long idUsuario,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            Pageable pageable);

    // Carga todo lo que usa la pantalla de detalle (open-in-view está desactivado).
    @EntityGraph(attributePaths = {"usuario", "tipoComida", "detalles", "detalles.alimento"})
    @Query("SELECT r FROM RegistroComida r WHERE r.idRegistro = :id")
    Optional<RegistroComida> findConDetallesById(@Param("id") Long id);

    // Para restaurar el celular: registros vigentes del usuario con todo lo que se envia
    // (tipo de comida, ingredientes y alimento), del mas reciente al mas antiguo.
    @EntityGraph(attributePaths = {"tipoComida", "detalles", "detalles.alimento"})
    @Query("""
        SELECT r FROM RegistroComida r
            WHERE r.usuario.idUsuario = :idUsuario AND r.eliminado = false
            ORDER BY r.fecha DESC, r.idRegistro DESC
    """)
    List<RegistroComida> findVigentesConDetalles(@Param("idUsuario") Long idUsuario);

    boolean existsByTipoComidaIdTipoComida(Integer idTipoComida);

    // Para la sincronizacion: busca un registro por el uuid que genero la app.
    Optional<RegistroComida> findByUuid(String uuid);
}
