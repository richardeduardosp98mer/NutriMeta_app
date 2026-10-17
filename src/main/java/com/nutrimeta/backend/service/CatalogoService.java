package com.nutrimeta.backend.service;

import com.nutrimeta.backend.repository.AlimentoRepositorio;
import com.nutrimeta.backend.repository.GrupoAlimentoRepositorio;
import com.nutrimeta.backend.repository.NivelActividadRepositorio;
import com.nutrimeta.backend.repository.TipoComidaRepositorio;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CatalogoService {

    private final AlimentoRepositorio alimentoRepositorio;
    private final GrupoAlimentoRepositorio grupoRepositorio;
    private final NivelActividadRepositorio nivelRepositorio;
    private final TipoComidaRepositorio tipoComidaRepositorio;

    public CatalogoService(AlimentoRepositorio alimentoRepositorio, GrupoAlimentoRepositorio grupoRepositorio,
                           NivelActividadRepositorio nivelRepositorio, TipoComidaRepositorio tipoComidaRepositorio) {
        this.alimentoRepositorio = alimentoRepositorio;
        this.grupoRepositorio = grupoRepositorio;
        this.nivelRepositorio = nivelRepositorio;
        this.tipoComidaRepositorio = tipoComidaRepositorio;
    }

    // Las tablas de referencia van siempre completas (son pocas filas).
    // Los alimentos van todos, o solo los modificados después de `desde`.
    @Transactional(readOnly = true)
    public CatalogoDto obtener(LocalDateTime desde) {
        // Se lee antes que los alimentos: si algo cambia entre las dos lecturas, vuelve a llegar en la
        // siguiente sincronización (repetir una fila no hace daño).
        LocalDateTime sincronizadoEn = alimentoRepositorio.ultimaActualizacion();

        return new CatalogoDto(
                sincronizadoEn,
                grupoRepositorio.findAll(Sort.by("idGrupo")).stream().map(CatalogoDto.GrupoDto::de).toList(),
                nivelRepositorio.findAll(Sort.by("orden")).stream().map(CatalogoDto.NivelActividadDto::de).toList(),
                tipoComidaRepositorio.findAll(Sort.by("orden")).stream().map(CatalogoDto.TipoComidaDto::de).toList(),
                alimentoRepositorio.findParaCatalogo(desde).stream().map(CatalogoDto.AlimentoDto::de).toList());
    }
}
