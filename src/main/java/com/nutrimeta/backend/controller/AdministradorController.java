package com.nutrimeta.backend.controller;

import com.nutrimeta.backend.entity.Administrador;
import com.nutrimeta.backend.repository.AdministradorRepositorio;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/portal/administradores")
public class AdministradorController {

    private final AdministradorRepositorio repositorio;
    private final PasswordEncoder passwordEncoder;

    public AdministradorController(AdministradorRepositorio repositorio, PasswordEncoder passwordEncoder) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("administradores", repositorio.findAll());
        return "portal/administradores/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("administrador", new Administrador());
        return "portal/administradores/formulario";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        model.addAttribute("administrador", repositorio.findById(id).orElseThrow());
        return "portal/administradores/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Administrador formulario, @RequestParam(required = false) String nuevaClave) {
        if (formulario.getIdAdministrador() == null) {
            // Nuevo administrador: la clave es obligatoria.
            formulario.setPasswordHash(passwordEncoder.encode(nuevaClave));
            repositorio.save(formulario);
        } else {
            Administrador existente = repositorio.findById(formulario.getIdAdministrador()).orElseThrow();
            existente.setNombre(formulario.getNombre());
            existente.setCorreo(formulario.getCorreo());
            existente.setActivo(formulario.getActivo());
            if (nuevaClave != null && !nuevaClave.isBlank()) {
                existente.setPasswordHash(passwordEncoder.encode(nuevaClave));
            }
            repositorio.save(existente);
        }
        return "redirect:/portal/administradores";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, Authentication authentication, Model model) {
        Administrador objetivo = repositorio.findById(id).orElseThrow();

        // CA-16-01: no puede desactivarse a sí mismo.
        if (objetivo.getCorreo().equals(authentication.getName())) {
            return mostrarError(model, "No puedes desactivarte a ti mismo.");
        }
        // CA-16-02: debe quedar al menos un administrador activo.
        if (repositorio.countByActivoTrue() <= 1) {
            return mostrarError(model, "Debe quedar al menos un administrador activo.");
        }

        objetivo.setActivo(false);
        repositorio.save(objetivo);
        return "redirect:/portal/administradores";
    }

    private String mostrarError(Model model, String mensaje) {
        model.addAttribute("administradores", repositorio.findAll());
        model.addAttribute("error", mensaje);
        return "portal/administradores/lista";
    }
}
