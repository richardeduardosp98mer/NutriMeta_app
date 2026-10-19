package com.nutrimeta.backend.service;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Respuesta de GET /api/sync/restaurar: lo que el servidor guarda del usuario,
// con la misma forma que usa POST /api/sync/subir.
public final class SyncRestaurarDto {

    private SyncRestaurarDto() {
    }

    public record PerfilItem(
            String sexo,
            LocalDate fechaNacimiento,
            BigDecimal pesoKg,
            BigDecimal tallaCm,
            String codigoNivelActividad,
            String objetivo,
            BigDecimal pesoObjetivoKg,
            Integer plazoSemanas,
            BigDecimal metaKcal,
            BigDecimal metaProteinaG,
            BigDecimal metaGrasaG,
            BigDecimal metaCarbohidratosG,
            Boolean metaAjustada,
            Integer version
    ) {
    }

    public record DetalleItem(
            Long idAlimento,
            BigDecimal cantidadG,
            BigDecimal energiaKcal,
            BigDecimal proteinaG,
            BigDecimal grasaG,
            BigDecimal carbohidratosG
    ) {
    }

    public record RegistroItem(
            String uuid,
            Integer idTipoComida,
            LocalDate fecha,
            Integer version,
            List<DetalleItem> detalles
    ) {
    }

    // perfil es null si el usuario todavia no subio el suyo. registros trae solo los vigentes
    // (no eliminados), del mas reciente al mas antiguo.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Respuesta(PerfilItem perfil, List<RegistroItem> registros) {
    }
}
