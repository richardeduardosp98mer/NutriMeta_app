package com.nutrimeta.backend.controller;

import com.nutrimeta.backend.entity.Perfil;
import com.nutrimeta.backend.entity.Usuario;
import com.nutrimeta.backend.repository.*;
import com.nutrimeta.backend.service.MetaService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/portal/usuarios")
public class UsuarioController {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PerfilRepositorio perfilRepositorio;
    private final NivelActividadRepositorio nivelRepositorio;
    private final MetaService metaService;

    public UsuarioController(UsuarioRepositorio usuarioRepositorio, PerfilRepositorio perfilRepositorio,
                             NivelActividadRepositorio nivelRepositorio, MetaService metaService) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.perfilRepositorio = perfilRepositorio;
        this.nivelRepositorio = nivelRepositorio;
        this.metaService = metaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String texto,
                         @RequestParam(defaultValue = "0") int pagina, Model model) {
        var resultado = usuarioRepositorio.buscar(
                (texto == null || texto.isBlank()) ? null : texto,
                PageRequest.of(pagina, 20, Sort.by("nombre")));
        model.addAttribute("pagina", resultado);
        model.addAttribute("texto", texto);
        return "portal/usuarios/lista";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioRepositorio.findById(id).orElseThrow();
        model.addAttribute("usuario", usuario);
        model.addAttribute("perfil", perfilRepositorio.findByUsuarioIdUsuario(id).orElse(null));
        model.addAttribute("niveles", nivelRepositorio.findAll());
        return "portal/usuarios/detalle";
    }

    @Transactional
    @PostMapping("/{id}/perfil/guardar")
    public String guardarPerfil(@PathVariable Long id, @ModelAttribute Perfil formulario,
                                RedirectAttributes redirect) {
        // Mismas reglas que el CHECK ck_perfil_objetivo de la BD: se avisa en pantalla en vez de fallar con 500.
        String error = validarPerfil(formulario);
        if (error != null) {
            redirect.addFlashAttribute("error", error);
            return "redirect:/portal/usuarios/" + id;
        }

        Perfil perfil = perfilRepositorio.findByUsuarioIdUsuario(id).orElseThrow();

        perfil.setPesoKg(formulario.getPesoKg());
        // El formulario solo trae el id del nivel: se carga completo para tener su factor.
        perfil.setNivelActividad(nivelRepositorio
                .findById(formulario.getNivelActividad().getIdNivelActividad()).orElseThrow());
        perfil.setObjetivo(formulario.getObjetivo());
        // Con MANTENER la BD exige peso objetivo y plazo en NULL.
        boolean mantener = formulario.getObjetivo() == Perfil.Objetivo.MANTENER;
        perfil.setPesoObjetivoKg(mantener ? null : formulario.getPesoObjetivoKg());
        perfil.setPlazoSemanas(mantener ? null : formulario.getPlazoSemanas());

        // CA-13-02: recalcula la meta con las mismas reglas RN-03 a RN-06.
        int edad = metaService.calcularEdad(perfil.getFechaNacimiento());
        BigDecimal tmb = metaService.calcularTmb(perfil.getSexo(), perfil.getPesoKg(), perfil.getTallaCm(), edad);
        BigDecimal get = metaService.calularGet(tmb, perfil.getNivelActividad().getFactor());
        var resultado = metaService.calcularMeta(
                perfil.getSexo(), perfil.getObjetivo(), perfil.getPesoKg(),
                perfil.getPesoObjetivoKg(), perfil.getPlazoSemanas(), get);

        perfil.setMetaKcal(resultado.metaKcal());
        perfil.setMetaProteinaG(resultado.metaProteinaG());
        perfil.setMetaGrasaG(resultado.metaGrasaG());
        perfil.setMetaCarbohidratosG(resultado.metaCarbohidratosG());
        perfil.setMetaAjustada(resultado.ajustada());
        perfil.setVersion(perfil.getVersion() + 1);

        perfilRepositorio.save(perfil);
        // Aviso push SYNC al usuario: lo dispara el paquete `firebase/` cuando R1 lo tenga listo.
        return "redirect:/portal/usuarios/" + id;
    }

    private String validarPerfil(Perfil f) {
        BigDecimal peso = f.getPesoKg();
        if (peso == null || peso.compareTo(new BigDecimal("30")) < 0 || peso.compareTo(new BigDecimal("300")) > 0) {
            return "El peso debe estar entre 30 y 300 kg.";
        }
        if (f.getObjetivo() == Perfil.Objetivo.MANTENER) {
            return null;
        }
        if (f.getPesoObjetivoKg() == null || f.getPlazoSemanas() == null || f.getPlazoSemanas() < 1) {
            return "Para bajar o subir de peso indica el peso objetivo y un plazo de al menos 1 semana.";
        }
        int comparacion = f.getPesoObjetivoKg().compareTo(peso);
        if (f.getObjetivo() == Perfil.Objetivo.BAJAR && comparacion >= 0) {
            return "Para bajar, el peso objetivo debe ser menor que el peso actual.";
        }
        if (f.getObjetivo() == Perfil.Objetivo.SUBIR && comparacion <= 0) {
            return "Para subir, el peso objetivo debe ser mayor que el peso actual.";
        }
        return null;
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Long id) { return cambiarEstado(id, true); }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id) {
        // CA-13-01: el servidor rechazará sus peticiones (eso lo aplica el filtro de /api/**, RN-14).
        return cambiarEstado(id, false);
    }

    private String cambiarEstado(Long id, boolean activo) {
        Usuario usuario = usuarioRepositorio.findById(id).orElseThrow();
        usuario.setActivo(activo);
        usuarioRepositorio.save(usuario);
        return "redirect:/portal/usuarios/" + id;
    }
}
