package com.nutrimeta.backend.service;

import com.nutrimeta.backend.entity.Alimento;
import com.nutrimeta.backend.entity.DetalleRegistro;
import com.nutrimeta.backend.entity.NivelActividad;
import com.nutrimeta.backend.entity.Perfil;
import com.nutrimeta.backend.entity.RegistroComida;
import com.nutrimeta.backend.entity.TipoComida;
import com.nutrimeta.backend.repository.AlimentoRepositorio;
import com.nutrimeta.backend.repository.NivelActividadRepositorio;
import com.nutrimeta.backend.repository.PerfilRepositorio;
import com.nutrimeta.backend.repository.RegistroComidaRepositorio;
import com.nutrimeta.backend.repository.TipoComidaRepositorio;
import com.nutrimeta.backend.repository.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

// Recibe lo que el celular tiene pendiente (perfil y registros de comida) y lo guarda.
// - Cada elemento se guarda en su propia transaccion: si uno falla, los demas se guardan igual.
// - Los registros se identifican por uuid: si ya existe se reemplaza completo (cabecera y detalle)
//   y la version la aumenta el servidor.
// - Un registro que el servidor ya tiene como eliminado no se revive.
@Service
public class SyncSubirService {

    private static final Logger log = LoggerFactory.getLogger(SyncSubirService.class);

    public static final String OK = "OK";
    public static final String RECHAZADO = "RECHAZADO";
    public static final String ERROR = "ERROR";

    private static final int MAX_INGREDIENTES = 50;
    private static final int MAX_PLAZO_SEMANAS = 1000;

    private static final BigDecimal CERO = BigDecimal.ZERO;
    private static final BigDecimal CANTIDAD_MAXIMA = new BigDecimal("2000");
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("100000");
    private static final BigDecimal PESO_MINIMO = new BigDecimal("30");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("300");
    private static final BigDecimal TALLA_MINIMA = new BigDecimal("120");
    private static final BigDecimal TALLA_MAXIMA = new BigDecimal("230");

    // Error de validacion: el elemento no se acepta y no tiene sentido reintentarlo.
    private static final class Rechazo extends RuntimeException {
        Rechazo(String mensaje) {
            super(mensaje, null, false, false);
        }
    }

    private final RegistroComidaRepositorio registros;
    private final PerfilRepositorio perfiles;
    private final UsuarioRepositorio usuarios;
    private final TipoComidaRepositorio tipos;
    private final AlimentoRepositorio alimentos;
    private final NivelActividadRepositorio niveles;
    private final TransactionTemplate transaccion;

    public SyncSubirService(RegistroComidaRepositorio registros,
                            PerfilRepositorio perfiles,
                            UsuarioRepositorio usuarios,
                            TipoComidaRepositorio tipos,
                            AlimentoRepositorio alimentos,
                            NivelActividadRepositorio niveles,
                            TransactionTemplate transaccion) {
        this.registros = registros;
        this.perfiles = perfiles;
        this.usuarios = usuarios;
        this.tipos = tipos;
        this.alimentos = alimentos;
        this.niveles = niveles;
        this.transaccion = transaccion;
    }

    public SyncSubirDto.Respuesta subir(Long idUsuario, SyncSubirDto.Solicitud solicitud) {

        SyncSubirDto.ResultadoItem resultadoPerfil = null;

        if (solicitud.perfil() != null) {
            resultadoPerfil = ejecutar(idUsuario, null, () -> guardarPerfil(idUsuario, solicitud.perfil()));
        }

        List<SyncSubirDto.ResultadoItem> resultadosRegistros = new ArrayList<>();

        if (solicitud.registros() != null) {
            for (SyncSubirDto.RegistroItem item : solicitud.registros()) {
                String uuid = item != null ? item.uuid() : null;
                resultadosRegistros.add(ejecutar(idUsuario, uuid, () -> guardarRegistro(idUsuario, item)));
            }
        }

        return new SyncSubirDto.Respuesta(
                java.time.LocalDateTime.now(), resultadoPerfil, resultadosRegistros);
    }

    private SyncSubirDto.ResultadoItem ejecutar(Long idUsuario, String uuid, Runnable trabajo) {
        try {
            transaccion.executeWithoutResult(estado -> trabajo.run());
            return new SyncSubirDto.ResultadoItem(uuid, OK, null);
        } catch (Rechazo e) {
            log.warn("Sincronizacion rechazada (usuario {}, {}): {}",
                    idUsuario, uuid != null ? "registro " + uuid : "perfil", e.getMessage());
            return new SyncSubirDto.ResultadoItem(uuid, RECHAZADO, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar un elemento de la sincronizacion (usuario {})", idUsuario, e);
            return new SyncSubirDto.ResultadoItem(uuid, ERROR, "Error del servidor; se reintentara");
        }
    }

    // ---------------------------------------------------------------- perfil

    private void guardarPerfil(Long idUsuario, SyncSubirDto.PerfilItem p) {

        Perfil.Sexo sexo = enumDe(Perfil.Sexo.class, p.sexo(), "Sexo no valido");
        Perfil.Objetivo objetivo = enumDe(Perfil.Objetivo.class, p.objetivo(), "Objetivo no valido");

        LocalDate nacimiento = p.fechaNacimiento();
        if (nacimiento == null || !nacimiento.isBefore(LocalDate.now())) {
            throw new Rechazo("Fecha de nacimiento no valida");
        }

        BigDecimal peso = enRango(p.pesoKg(), PESO_MINIMO, PESO_MAXIMO, 2, "Peso fuera de rango");
        BigDecimal talla = enRango(p.tallaCm(), TALLA_MINIMA, TALLA_MAXIMA, 1, "Talla fuera de rango");

        if (p.codigoNivelActividad() == null || p.codigoNivelActividad().isBlank()) {
            throw new Rechazo("Nivel de actividad obligatorio");
        }

        NivelActividad nivel = niveles
                .findByCodigo(p.codigoNivelActividad().trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new Rechazo("Nivel de actividad inexistente"));

        BigDecimal pesoObjetivo = null;
        Integer plazo = null;

        // Con MANTENER no hay peso objetivo ni plazo (lo exige la tabla). Con BAJAR o
        // SUBIR son opcionales, pero van juntos; si vienen se validan.
        if (objetivo != Perfil.Objetivo.MANTENER
                && (p.pesoObjetivoKg() != null || p.plazoSemanas() != null)) {

            if (p.pesoObjetivoKg() == null || p.plazoSemanas() == null) {
                throw new Rechazo("Peso objetivo y plazo deben enviarse juntos");
            }

            pesoObjetivo = enRango(p.pesoObjetivoKg(), PESO_MINIMO, PESO_MAXIMO, 2,
                    "Peso objetivo fuera de rango");
            plazo = p.plazoSemanas();

            if (plazo < 1 || plazo > MAX_PLAZO_SEMANAS) {
                throw new Rechazo("Plazo en semanas no valido");
            }

            int comparacion = pesoObjetivo.compareTo(peso);

            if (objetivo == Perfil.Objetivo.BAJAR && comparacion >= 0) {
                throw new Rechazo("Para bajar, el peso objetivo debe ser menor al peso actual");
            }

            if (objetivo == Perfil.Objetivo.SUBIR && comparacion <= 0) {
                throw new Rechazo("Para subir, el peso objetivo debe ser mayor al peso actual");
            }
        }

        BigDecimal metaKcal = noNegativo(p.metaKcal(), 1, "Meta de calorias no valida");
        BigDecimal metaProteina = noNegativo(p.metaProteinaG(), 1, "Meta de proteina no valida");
        BigDecimal metaGrasa = noNegativo(p.metaGrasaG(), 1, "Meta de grasa no valida");
        BigDecimal metaCarbohidratos = noNegativo(p.metaCarbohidratosG(), 1, "Meta de carbohidratos no valida");

        Perfil perfil = perfiles.findByUsuarioIdUsuario(idUsuario).orElse(null);

        if (perfil == null) {
            perfil = new Perfil();
            perfil.setUsuario(usuarios.getReferenceById(idUsuario));
            perfil.setVersion(1);
        } else {
            perfil.setVersion(perfil.getVersion() + 1);
        }

        perfil.setSexo(sexo);
        perfil.setFechaNacimiento(nacimiento);
        perfil.setPesoKg(peso);
        perfil.setTallaCm(talla);
        perfil.setNivelActividad(nivel);
        perfil.setObjetivo(objetivo);
        perfil.setPesoObjetivoKg(pesoObjetivo);
        perfil.setPlazoSemanas(plazo);
        perfil.setMetaKcal(metaKcal);
        perfil.setMetaProteinaG(metaProteina);
        perfil.setMetaGrasaG(metaGrasa);
        perfil.setMetaCarbohidratosG(metaCarbohidratos);
        perfil.setMetaAjustada(Boolean.TRUE.equals(p.metaAjustada()));

        perfiles.save(perfil);
    }

    // -------------------------------------------------------------- registro

    private void guardarRegistro(Long idUsuario, SyncSubirDto.RegistroItem r) {

        if (r == null) {
            throw new Rechazo("Registro vacio");
        }

        String uuid = uuidValido(r.uuid());
        boolean eliminar = Boolean.TRUE.equals(r.eliminado());

        Optional<RegistroComida> existente = registros.findByUuid(uuid);

        if (existente.isPresent()) {

            RegistroComida registro = existente.get();

            if (!idUsuario.equals(registro.getUsuario().getIdUsuario())) {
                throw new Rechazo("El registro pertenece a otro usuario");
            }

            if (Boolean.TRUE.equals(registro.getEliminado())) {
                if (eliminar) {
                    return; // ya estaba eliminado: nada que hacer
                }
                throw new Rechazo("El registro fue eliminado en el servidor");
            }

            if (eliminar) {
                registro.setEliminado(true);
            } else {
                aplicar(registro, r);
            }

            registro.setVersion(registro.getVersion() + 1);
            registros.save(registro);
            return;
        }

        if (eliminar) {
            return; // creado y eliminado antes de subirse: el servidor nunca lo conocio
        }

        RegistroComida nuevo = new RegistroComida();
        nuevo.setUuid(uuid);
        nuevo.setUsuario(usuarios.getReferenceById(idUsuario));
        nuevo.setVersion(1);
        nuevo.setEliminado(false);
        aplicar(nuevo, r);
        registros.save(nuevo);
    }

    // Valida todo primero y recien despues modifica el registro.
    private void aplicar(RegistroComida registro, SyncSubirDto.RegistroItem r) {

        if (r.idTipoComida() == null) {
            throw new Rechazo("Tipo de comida obligatorio");
        }

        TipoComida tipo = tipos.findById(r.idTipoComida())
                .orElseThrow(() -> new Rechazo("Tipo de comida inexistente"));

        if (r.fecha() == null) {
            throw new Rechazo("Fecha obligatoria");
        }

        List<SyncSubirDto.DetalleItem> items = r.detalles();

        if (items == null || items.isEmpty()) {
            throw new Rechazo("El registro no tiene ingredientes");
        }

        if (items.size() > MAX_INGREDIENTES) {
            throw new Rechazo("Demasiados ingredientes");
        }

        List<DetalleRegistro> detalles = new ArrayList<>();

        for (SyncSubirDto.DetalleItem d : items) {

            if (d == null || d.idAlimento() == null) {
                throw new Rechazo("Ingrediente sin alimento");
            }

            Alimento alimento = alimentos.findById(d.idAlimento())
                    .orElseThrow(() -> new Rechazo("Alimento inexistente: " + d.idAlimento()));

            BigDecimal cantidad = d.cantidadG() == null ? null : d.cantidadG().setScale(2, RoundingMode.HALF_UP);

            if (cantidad == null || cantidad.compareTo(CERO) <= 0 || cantidad.compareTo(CANTIDAD_MAXIMA) > 0) {
                throw new Rechazo("Cantidad fuera de rango (0 a 2000)");
            }

            DetalleRegistro detalle = new DetalleRegistro();
            detalle.setRegistro(registro);
            detalle.setAlimento(alimento);
            detalle.setCantidadG(cantidad);
            detalle.setEnergiaKcal(noNegativo(d.energiaKcal(), 2, "Energia no valida"));
            detalle.setProteinaG(noNegativo(d.proteinaG(), 2, "Proteina no valida"));
            detalle.setGrasaG(noNegativo(d.grasaG(), 2, "Grasa no valida"));
            detalle.setCarbohidratosG(noNegativo(d.carbohidratosG(), 2, "Carbohidratos no validos"));
            detalles.add(detalle);
        }

        registro.setTipoComida(tipo);
        registro.setFecha(r.fecha());
        registro.getDetalles().clear();
        registro.getDetalles().addAll(detalles);
    }

    // --------------------------------------------------------------- ayudas

    private static String uuidValido(String valor) {

        if (valor == null || valor.length() != 36) {
            throw new Rechazo("uuid no valido");
        }

        try {
            return UUID.fromString(valor).toString();
        } catch (IllegalArgumentException e) {
            throw new Rechazo("uuid no valido");
        }
    }

    private static <E extends Enum<E>> E enumDe(Class<E> tipo, String valor, String mensaje) {

        if (valor == null) {
            throw new Rechazo(mensaje);
        }

        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new Rechazo(mensaje);
        }
    }

    private static BigDecimal enRango(BigDecimal valor, BigDecimal minimo, BigDecimal maximo,
                                      int decimales, String mensaje) {

        if (valor == null) {
            throw new Rechazo(mensaje);
        }

        BigDecimal redondeado = valor.setScale(decimales, RoundingMode.HALF_UP);

        if (redondeado.compareTo(minimo) < 0 || redondeado.compareTo(maximo) > 0) {
            throw new Rechazo(mensaje);
        }

        return redondeado;
    }

    private static BigDecimal noNegativo(BigDecimal valor, int decimales, String mensaje) {

        if (valor == null) {
            throw new Rechazo(mensaje);
        }

        BigDecimal redondeado = valor.setScale(decimales, RoundingMode.HALF_UP);

        if (redondeado.compareTo(CERO) < 0 || redondeado.compareTo(VALOR_MAXIMO) >= 0) {
            throw new Rechazo(mensaje);
        }

        return redondeado;
    }
}
