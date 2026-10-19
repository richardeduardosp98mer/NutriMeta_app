package com.nutrimeta.backend.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Cuerpo y respuesta de POST /api/sync/subir
public final class SyncSubirDto {

    private SyncSubirDto() {
    }

    // perfil va solo si el celular lo tiene pendiente; registros son los pendientes de subir.
    public record Solicitud(
            PerfilItem perfil,
            @Size(max = 200) List<RegistroItem> registros
    ) {
    }

    // El nivel de actividad viaja por su codigo (SEDENTARIO, LIGERO, ...), igual en app y servidor.
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
            Boolean metaAjustada
    ) {
    }

    public record RegistroItem(
            String uuid,
            Integer idTipoComida,
            LocalDate fecha,
            Boolean eliminado,
            List<DetalleItem> detalles
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

    // estado: OK = guardado; RECHAZADO = el servidor no lo acepta (no reintentar);
    // ERROR = fallo del servidor (reintentar en la proxima sincronizacion).
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ResultadoItem(String uuid, String estado, String motivo) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Respuesta(
            LocalDateTime sincronizadoEn,
            ResultadoItem perfil,
            List<ResultadoItem> registros
    ) {
    }
}
