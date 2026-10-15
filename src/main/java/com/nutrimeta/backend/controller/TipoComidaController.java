package com.nutrimeta.backend.controller;

import com.nutrimeta.backend.entity.TipoComida;
import com.nutrimeta.backend.repository.RegistroComidaRepositorio;
import com.nutrimeta.backend.repository.TipoComidaRepositorio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/portal/tipos-comida")
public class TipoComidaController {

    private final TipoComidaRepositorio repositorio;
    private final RegistroComidaRepositorio registroComidaRepositorio;

    public TipoComidaController(TipoComidaRepositorio repositorio, RegistroComidaRepositorio registroComidaRepositorio) {
        this.repositorio = repositorio;
        this.registroComidaRepositorio = registroComidaRepositorio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("tipos", repositorio.findAll());
        return "portal/tipos-comida/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("tipo", new TipoComida());
        return "portal/tipos-comida/formulario";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable Integer id, Model model) {
        model.addAttribute("tipo", repositorio.findById(id).orElseThrow());
        return "portal/tipos-comida/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute TipoComida tipo) {
        repositorio.save(tipo);
        return "redirect:/portal/tipos-comida";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Integer id, Model model) {
        // CA-15-01: no se puede eliminar un tipo de comida en uso.
        if (registroComidaRepositorio.existsByTipoComidaIdTipoComida(id)) {
            model.addAttribute("tipos", repositorio.findAll());
            model.addAttribute("error", "No se puede eliminar: tiene registros de comida asociados.");
            return "portal/tipos-comida/lista";
        }
        repositorio.deleteById(id);
        return "redirect:/portal/tipos-comida";
    }
}