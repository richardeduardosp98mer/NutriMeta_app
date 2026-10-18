package com.nutrimeta.backend.service;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

// Cuerpo y respuesta de POST /api/usuarios/me
public final class UsuarioApiDto {

    private UsuarioApiDto() {
    }

    // Todos los campos son opcionales. La aceptacion de terminos solo se usa al crear el usuario.
    public record Solicitud(
            @Size(max = 100) String nombre,
            LocalDateTime fechaAceptacionTerminos,
            @Size(max = 10) String versionTerminos
    ) {
    }

    public record Respuesta(
            Long idUsuario,
            String firebaseUid,
            String correo,
            String nombre,
            boolean activo,
            boolean creado
    ) {
    }
}
