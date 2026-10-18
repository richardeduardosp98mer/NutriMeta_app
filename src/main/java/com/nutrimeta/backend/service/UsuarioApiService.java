package com.nutrimeta.backend.service;

import com.nutrimeta.backend.entity.Usuario;
import com.nutrimeta.backend.repository.UsuarioRepositorio;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UsuarioApiService {

    private static final String VERSION_TERMINOS_INICIAL = "1.0";

    // `usuario` es el que se devuelve; `creado` indica si se acaba de crear.
    public record Resultado(Usuario usuario, boolean creado) {
    }

    private final UsuarioRepositorio repositorio;

    public UsuarioApiService(UsuarioRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // Crea el usuario la primera vez que se autentica y, si ya existe, lo recupera.
    // - uid y correo salen del token verificado.
    // - nombreSolicitado: el que envio la app (puede ser null). Si no hay, al crear se usa el del
    //   token o, en su defecto, la parte del correo antes de la @. Al recuperar, el nombre solo
    //   se actualiza si la app lo envio.
    // - La aceptacion de terminos solo se registra al crear (si no viene, fecha actual y "1.0").
    // Un usuario desactivado se devuelve tal cual, sin modificarlo; el controlador responde 403.
    public Resultado registrar(String firebaseUid, String correo, String nombreToken,
                               String nombreSolicitado, LocalDateTime fechaTerminos, String versionTerminos) {

        Optional<Usuario> existente = repositorio.findByFirebaseUid(firebaseUid);

        if (existente.isPresent()) {
            return new Resultado(actualizar(existente.get(), correo, nombreSolicitado), false);
        }

        Usuario nuevo = new Usuario();
        nuevo.setFirebaseUid(firebaseUid);
        nuevo.setCorreo(correo);
        nuevo.setNombre(nombreParaCrear(nombreSolicitado, nombreToken, correo));
        nuevo.setFechaAceptacionTerminos(fechaTerminos != null ? fechaTerminos : LocalDateTime.now());
        nuevo.setVersionTerminos(versionTerminos != null && !versionTerminos.isBlank()
                ? versionTerminos.trim() : VERSION_TERMINOS_INICIAL);

        try {
            return new Resultado(repositorio.saveAndFlush(nuevo), true);
        } catch (DataIntegrityViolationException e) {
            // Otra llamada al mismo tiempo ya lo creo (uid unico): se recupera ese usuario.
            Usuario creadoPorOtra = repositorio.findByFirebaseUid(firebaseUid).orElseThrow(() -> e);
            return new Resultado(creadoPorOtra, false);
        }
    }

    private Usuario actualizar(Usuario usuario, String correo, String nombreSolicitado) {

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            return usuario;
        }

        boolean cambio = false;

        if (!correo.equals(usuario.getCorreo())) {
            usuario.setCorreo(correo);
            cambio = true;
        }

        if (nombreSolicitado != null && !nombreSolicitado.isBlank()
                && !nombreSolicitado.trim().equals(usuario.getNombre())) {
            usuario.setNombre(nombreSolicitado.trim());
            cambio = true;
        }

        return cambio ? repositorio.save(usuario) : usuario;
    }

    private String nombreParaCrear(String solicitado, String delToken, String correo) {

        String nombre = primeroNoVacio(solicitado, delToken);

        if (nombre == null) {
            int arroba = correo.indexOf('@');
            nombre = arroba > 0 ? correo.substring(0, arroba) : correo;
        }

        nombre = nombre.trim();

        // La columna nombre admite 100 caracteres.
        return nombre.length() > 100 ? nombre.substring(0, 100) : nombre;
    }

    private String primeroNoVacio(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b != null && !b.isBlank() ? b : null;
    }
}
