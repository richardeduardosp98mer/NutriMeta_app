package com.nutrimeta.backend.controller;

import com.nutrimeta.backend.entity.Usuario;
import com.nutrimeta.backend.service.UsuarioApiDto;
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
@RequestMapping("/api/usuarios")
public class UsuarioApiController {

    private final UsuarioApiService servicio;

    public UsuarioApiController(UsuarioApiService servicio) {
        this.servicio = servicio;
    }

    // POST /api/usuarios/me
    // Authorization: Bearer <ID token de Firebase>. El uid y el correo salen del token, no del cuerpo.
    // 201 = usuario creado, 200 = ya existia, 403 = usuario desactivado, 401 = token invalido (lo responde Spring Security).
    @PostMapping("/me")
    public ResponseEntity<?> registrarme(@AuthenticationPrincipal Jwt token,
                                         @Valid @RequestBody(required = false) UsuarioApiDto.Solicitud cuerpo) {

        String correo = token.getClaimAsString("email");

        if (correo == null || correo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token no trae correo");
        }

        UsuarioApiDto.Solicitud solicitud = cuerpo != null
                ? cuerpo
                : new UsuarioApiDto.Solicitud(null, null, null);

        UsuarioApiService.Resultado resultado = servicio.registrar(
                token.getSubject(),
                correo,
                token.getClaimAsString("name"),
                solicitud.nombre(),
                solicitud.fechaAceptacionTerminos(),
                solicitud.versionTerminos());

        Usuario usuario = resultado.usuario();

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "usuario_inactivo"));
        }

        UsuarioApiDto.Respuesta respuesta = new UsuarioApiDto.Respuesta(
                usuario.getIdUsuario(),
                usuario.getFirebaseUid(),
                usuario.getCorreo(),
                usuario.getNombre(),
                true,
                resultado.creado());

        return ResponseEntity
                .status(resultado.creado() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(respuesta);
    }
}
