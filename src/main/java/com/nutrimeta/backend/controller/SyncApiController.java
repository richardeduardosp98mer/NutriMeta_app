package com.nutrimeta.backend.controller;

import com.nutrimeta.backend.entity.Usuario;
import com.nutrimeta.backend.service.SyncRestaurarService;
import com.nutrimeta.backend.service.SyncSubirDto;
import com.nutrimeta.backend.service.SyncSubirService;
import com.nutrimeta.backend.service.UsuarioApiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final SyncRestaurarService restauracion;

    public SyncApiController(UsuarioApiService usuarioServicio,
                             SyncSubirService sincronizacion,
                             SyncRestaurarService restauracion) {
        this.usuarioServicio = usuarioServicio;
        this.sincronizacion = sincronizacion;
        this.restauracion = restauracion;
    }

    // POST /api/sync/subir
    // Authorization: Bearer <ID token de Firebase>. El usuario sale del token, no del cuerpo.
    // 200 = procesado (el resultado de cada elemento va en el cuerpo), 400 = cuerpo mal formado
    // o demasiados registros, 401 = token invalido, 403 = usuario desactivado.
    @PostMapping("/subir")
    public ResponseEntity<?> subir(@AuthenticationPrincipal Jwt token,
                                   @Valid @RequestBody SyncSubirDto.Solicitud cuerpo) {

        Usuario usuario = usuarioDelToken(token);

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "usuario_inactivo"));
        }

        return ResponseEntity.ok(sincronizacion.subir(usuario.getIdUsuario(), cuerpo));
    }

    // GET /api/sync/restaurar
    // Devuelve el perfil y los registros vigentes del usuario del token, para repoblar un celular
    // sin datos locales. 200 = ok (perfil ausente si nunca subio uno), 401 = token invalido,
    // 403 = usuario desactivado.
    @GetMapping("/restaurar")
    public ResponseEntity<?> restaurar(@AuthenticationPrincipal Jwt token) {

        Usuario usuario = usuarioDelToken(token);

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "usuario_inactivo"));
        }

        return ResponseEntity.ok(restauracion.restaurar(usuario.getIdUsuario()));
    }

    // Si todavia no existe en el backend se crea igual que en /api/usuarios/me,
    // asi el orden entre las llamadas no importa.
    private Usuario usuarioDelToken(Jwt token) {

        String correo = token.getClaimAsString("email");

        if (correo == null || correo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token no trae correo");
        }

        return usuarioServicio.registrar(
                token.getSubject(),
                correo,
                token.getClaimAsString("name"),
                null,
                null,
                null).usuario();
    }
}
