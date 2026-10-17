package com.nutrimeta.backend.controller;

import com.nutrimeta.backend.entity.Alimento;
import com.nutrimeta.backend.entity.DetalleRegistro;
import com.nutrimeta.backend.entity.RegistroComida;
import com.nutrimeta.backend.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Controller
@RequestMapping("/portal/registros")
public class RegistroComidaController {

    private final RegistroComidaRepositorio repositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final AlimentoRepositorio alimentoRepositorio;
    private final TipoComidaRepositorio tipoComidaRepositorio;

    public RegistroComidaController(RegistroComidaRepositorio repositorio, UsuarioRepositorio usuarioRepositorio,
                                    AlimentoRepositorio alimentoRepositorio, TipoComidaRepositorio tipoComidaRepositorio) {
        this.repositorio = repositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.alimentoRepositorio = alimentoRepositorio;
        this.tipoComidaRepositorio = tipoComidaRepositorio;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Long idUsuario,
                         @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate hasta,
                         @RequestParam(defaultValue = "0") int pagina, Model model) {

        var resultado = repositorio.buscar(idUsuario, desde, hasta, PageRequest.of(pagina, 20, Sort.by("fecha").descending()));
        model.addAttribute("pagina", resultado);
        model.addAttribute("usuarios", usuarioRepositorio.findAll());
        model.addAttribute("idUsuario", idUsuario);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "portal/registros/lista";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("registro", repositorio.findConDetallesById(id).orElseThrow());
        model.addAttribute("tiposComida", tipoComidaRepositorio.findAll());
        return "portal/registros/detalle";
    }

    @Transactional
    @PostMapping("/{id}/guardar")
    public String guardar(@PathVariable Long id, @RequestParam Integer idTipoComida, @RequestParam LocalDate fecha,
                          @RequestParam java.util.List<Long> idDetalle, @RequestParam java.util.List<BigDecimal> cantidadG) {

        RegistroComida registro = repositorio.findConDetallesById(id).orElseThrow();
        registro.setTipoComida(tipoComidaRepositorio.findById(idTipoComida).orElseThrow());
        registro.setFecha(fecha);

        for (int i = 0; i < idDetalle.size(); i++) {
            Long detalleId = idDetalle.get(i);
            BigDecimal nuevaCantidad = cantidadG.get(i);

            DetalleRegistro detalle = registro.getDetalles().stream()
                    .filter(d -> d.getIdDetalle().equals(detalleId))
                    .findFirst().orElseThrow();

            // RN-07: recalcula solo si la cantidad cambió.
            if (detalle.getCantidadG().compareTo(nuevaCantidad) != 0) {
                Alimento alimento = detalle.getAlimento();
                detalle.setCantidadG(nuevaCantidad);
                detalle.setEnergiaKcal(porcentaje(alimento.getEnergiaKcal(), nuevaCantidad));
                detalle.setProteinaG(porcentaje(alimento.getProteinaG(), nuevaCantidad));
                detalle.setGrasaG(porcentaje(alimento.getGrasaG(), nuevaCantidad));
                detalle.setCarbohidratosG(porcentaje(alimento.getCarbohidratosG(), nuevaCantidad));
            }
        }

        registro.setVersion(registro.getVersion() + 1);
        repositorio.save(registro);
        // Aviso push SYNC al usuario dueño: lo dispara el paquete `firebase/` cuando R1 lo tenga listo.
        return "redirect:/portal/registros/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id) {
        RegistroComida registro = repositorio.findById(id).orElseThrow();
        // CA-14-02: marca eliminado, nunca borra físicamente desde el portal (RN-12).
        registro.setEliminado(true);
        registro.setVersion(registro.getVersion() + 1);
        repositorio.save(registro);
        return "redirect:/portal/registros";
    }

    private BigDecimal porcentaje(BigDecimal valorPor100g, BigDecimal cantidadG) {
        return valorPor100g.multiply(cantidadG).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
