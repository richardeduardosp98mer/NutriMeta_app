package com.nutrimeta.backend.controller;

import com.nutrimeta.backend.entity.Usuario;
import com.nutrimeta.backend.service.SyncSubirDto;
import com.nutrimeta.backend.service.SyncSubirService;
import com.nutrimeta.backend.service.UsuarioApiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncApiController {

    private final UsuarioApiService usuarioServicio;
    private final SyncSubirService sincronizacion;

    public SyncApiController(UsuarioApiService usuarioServicio, SyncSubirService sincronizacion) {
        this.usuarioServicio = usuarioServicio;
        this.sincronizacion = sincronizacion;
    }

    // POST /api/sync/subir
    // Authorization: Bearer <ID token de Firebase>. El usuario sale del token, no del cuerpo.
    // 200 = procesado (el resultado de cada elemento va en el cuerpo), 400 = cuerpo mal formado
    // o demasiados registros, 401 = token invalido, 403 = usuario desactivado.
    @PostMapping("/subir")
    public ResponseEntity<?> subir(@AuthenticationPrincipal Jwt token,
                                   @Valid @RequestBody SyncSubirDto.Solicitud cuerpo) {

        String correo = token.getClaimAsString("email");

        if (correo == null || correo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token no trae correo");
        }

        // Si todavia no existe en el backend se crea igual que en /api/usuarios/me,
        // asi el orden entre ambas llamadas no importa.
        Usuario usuario = usuarioServicio.registrar(
                token.getSubject(),
                correo,
                token.getClaimAsString("name"),
                null,
                null,
                null).usuario();

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "usuario_inactivo"));
        }

        return ResponseEntity.ok(sincronizacion.subir(usuario.getIdUsuario(), cuerpo));
    }
}
