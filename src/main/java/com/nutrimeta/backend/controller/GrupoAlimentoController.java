package com.nutrimeta.backend.controller;

import com.nutrimeta.backend.entity.GrupoAlimento;
import com.nutrimeta.backend.repository.AlimentoRepositorio;
import com.nutrimeta.backend.repository.GrupoAlimentoRepositorio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/portal/grupos")
public class GrupoAlimentoController {

    private final GrupoAlimentoRepositorio repositorio;
    private final AlimentoRepositorio alimentoRepositorio;

    public GrupoAlimentoController(GrupoAlimentoRepositorio repositorio, AlimentoRepositorio alimentoRepositorio) {
        this.repositorio = repositorio;
        this.alimentoRepositorio = alimentoRepositorio;
    }

    @GetMapping
    public String listar(Model model){
        model.addAttribute("grupos", repositorio.findAll());
        return "portal/grupos/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("grupo", new GrupoAlimento());
        return "portal/grupos/formulario";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable Integer id, Model model) {
        model.addAttribute("grupo", repositorio.findById(id).orElseThrow());
        return "portal/grupos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute GrupoAlimento grupo) {
        repositorio.save(grupo);
        return "redirect:/portal/grupos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Integer id, Model model) {
        // no se puede eliminar un grupo en uso.
        if (alimentoRepositorio.existsByGrupoIdGrupo(id)) {
            model.addAttribute("grupos", repositorio.findAll());
            model.addAttribute("error", "No se puede eliminar: tiene alimentos asociados.");
            return "portal/grupos/lista";
        }
        repositorio.deleteById(id);
        return "redirect:/portal/grupos";
    }
}
