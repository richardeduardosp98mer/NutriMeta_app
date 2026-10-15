package com.nutrimeta.backend.controller;

import com.nutrimeta.backend.entity.NivelActividad;
import com.nutrimeta.backend.repository.NivelActividadRepositorio;
import com.nutrimeta.backend.repository.PerfilRepositorio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/portal/niveles-actividad")
public class NivelActividadController {

    private final NivelActividadRepositorio repositorio;
    private final PerfilRepositorio perfilRepositorio;

    public NivelActividadController(NivelActividadRepositorio repositorio, PerfilRepositorio perfilRepositorio) {
        this.repositorio = repositorio;
        this.perfilRepositorio = perfilRepositorio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("niveles", repositorio.findAll());
        return "portal/niveles-actividad/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("nivel", new NivelActividad());
        return "portal/niveles-actividad/formulario";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable Integer id, Model model) {
        model.addAttribute("nivel", repositorio.findById(id).orElseThrow());
        return "portal/niveles-actividad/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute NivelActividad nivel, Model model) {
        if (nivel.getFactor().compareTo(new BigDecimal("1.0")) < 0
                || nivel.getFactor().compareTo(new BigDecimal("2.5")) > 0) {
            model.addAttribute("nivel", nivel);
            model.addAttribute("error", "El factor debe estar entre 1.0 y 2.5.");
            return "portal/niveles-actividad/formulario";
        }
        repositorio.save(nivel);
        return "redirect:/portal/niveles-actividad";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Integer id, Model model) {
        if (perfilRepositorio.existsByNivelActividadIdNivelActividad(id)) {
            model.addAttribute("niveles", repositorio.findAll());
            model.addAttribute("error", "No se puede eliminar: tiene usuarios con perfiles asociados.");
            return "portal/niveles-actividad/lista";
        }
        repositorio.deleteById(id);
        return "redirect:/portal/niveles-actividad";
    }
}