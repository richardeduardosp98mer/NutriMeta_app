package com.nutrimeta.backend.service;

import com.nutrimeta.backend.entity.DetalleRegistro;
import com.nutrimeta.backend.entity.Perfil;
import com.nutrimeta.backend.entity.RegistroComida;
import com.nutrimeta.backend.repository.PerfilRepositorio;
import com.nutrimeta.backend.repository.RegistroComidaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

// Devuelve al celular lo que el servidor tiene del usuario (perfil y registros vigentes).
// El celular lo usa solo cuando su base local esta vacia (reinstalacion o celular nuevo).
@Service
public class SyncRestaurarService {

    // Tope de registros por respuesta, para no devolver cantidades enormes de una vez.
    private static final int MAX_REGISTROS = 2000;

    private final PerfilRepositorio perfiles;
    private final RegistroComidaRepositorio registros;

    public SyncRestaurarService(PerfilRepositorio perfiles, RegistroComidaRepositorio registros) {
        this.perfiles = perfiles;
        this.registros = registros;
    }

    @Transactional(readOnly = true)
    public SyncRestaurarDto.Respuesta restaurar(Long idUsuario) {

        SyncRestaurarDto.PerfilItem perfil = perfiles.findByUsuarioIdUsuario(idUsuario)
                .map(this::aPerfil)
                .orElse(null);

        List<SyncRestaurarDto.RegistroItem> lista = registros.findVigentesConDetalles(idUsuario)
                .stream()
                .limit(MAX_REGISTROS)
                .map(this::aRegistro)
                .toList();

        return new SyncRestaurarDto.Respuesta(perfil, lista);
    }

    private SyncRestaurarDto.PerfilItem aPerfil(Perfil p) {
        return new SyncRestaurarDto.PerfilItem(
                p.getSexo().name(),
                p.getFechaNacimiento(),
                p.getPesoKg(),
                p.getTallaCm(),
                p.getNivelActividad().getCodigo(),
                p.getObjetivo().name(),
                p.getPesoObjetivoKg(),
                p.getPlazoSemanas(),
                p.getMetaKcal(),
                p.getMetaProteinaG(),
                p.getMetaGrasaG(),
                p.getMetaCarbohidratosG(),
                p.getMetaAjustada(),
                p.getVersion()
        );
    }

    private SyncRestaurarDto.RegistroItem aRegistro(RegistroComida r) {

        List<SyncRestaurarDto.DetalleItem> detalles = r.getDetalles().stream()
                .sorted(Comparator.comparing(DetalleRegistro::getIdDetalle))
                .map(d -> new SyncRestaurarDto.DetalleItem(
                        d.getAlimento().getIdAlimento(),
                        d.getCantidadG(),
                        d.getEnergiaKcal(),
                        d.getProteinaG(),
                        d.getGrasaG(),
                        d.getCarbohidratosG()))
                .toList();

        return new SyncRestaurarDto.RegistroItem(
                r.getUuid(),
                r.getTipoComida().getIdTipoComida(),
                r.getFecha(),
                r.getVersion(),
                detalles
        );
    }
}
