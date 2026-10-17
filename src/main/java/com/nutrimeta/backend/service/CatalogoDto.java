package com.nutrimeta.backend.service;

import com.nutrimeta.backend.entity.Alimento;
import com.nutrimeta.backend.entity.GrupoAlimento;
import com.nutrimeta.backend.entity.NivelActividad;
import com.nutrimeta.backend.entity.TipoComida;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Respuesta de GET /api/catalogo. Los nombres de los campos son el contrato con la app.
public record CatalogoDto(
        LocalDateTime sincronizadoEn,
        List<GrupoDto> grupos,
        List<NivelActividadDto> nivelesActividad,
        List<TipoComidaDto> tiposComida,
        List<AlimentoDto> alimentos) {

    public record GrupoDto(Integer idGrupo, String codigo, String nombre) {
        static GrupoDto de(GrupoAlimento g) {
            return new GrupoDto(g.getIdGrupo(), g.getCodigo(), g.getNombre());
        }
    }

    public record NivelActividadDto(Integer idNivelActividad, String codigo, String nombre,
                                    String descripcion, BigDecimal factor, Integer orden) {
        static NivelActividadDto de(NivelActividad n) {
            return new NivelActividadDto(n.getIdNivelActividad(), n.getCodigo(), n.getNombre(),
                    n.getDescripcion(), n.getFactor(), n.getOrden());
        }
    }

    public record TipoComidaDto(Integer idTipoComida, String nombre, Integer orden) {
        static TipoComidaDto de(TipoComida t) {
            return new TipoComidaDto(t.getIdTipoComida(), t.getNombre(), t.getOrden());
        }
    }

    // fibraG llega como null cuando el INS no la reporta (null no es lo mismo que 0).
    public record AlimentoDto(Long idAlimento, String codigoIns, Integer idGrupo, String nombre,
                              Alimento.Unidad unidad, BigDecimal energiaKcal, BigDecimal proteinaG,
                              BigDecimal grasaG, BigDecimal carbohidratosG, BigDecimal fibraG,
                              Alimento.MetodoCarbohidratos metodoCarbohidratos, Alimento.Fuente fuente,
                              boolean activo) {
        static AlimentoDto de(Alimento a) {
            return new AlimentoDto(a.getIdAlimento(), a.getCodigoIns(), a.getGrupo().getIdGrupo(),
                    a.getNombre(), a.getUnidad(), a.getEnergiaKcal(), a.getProteinaG(), a.getGrasaG(),
                    a.getCarbohidratosG(), a.getFibraG(), a.getMetodoCarbohidratos(), a.getFuente(),
                    Boolean.TRUE.equals(a.getActivo()));
        }
    }
}
