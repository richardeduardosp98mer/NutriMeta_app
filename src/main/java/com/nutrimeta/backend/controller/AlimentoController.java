package com.nutrimeta.backend.controller;

import com.nutrimeta.backend.entity.Alimento;
import com.nutrimeta.backend.repository.AlimentoRepositorio;
import com.nutrimeta.backend.repository.GrupoAlimentoRepositorio;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/portal/alimentos")
public class AlimentoController {

    private final AlimentoRepositorio repositorio;
    private final GrupoAlimentoRepositorio grupoRepositorio;

    public AlimentoController(AlimentoRepositorio repositorio, GrupoAlimentoRepositorio grupoRepositorio) {
        this.repositorio = repositorio;
        this.grupoRepositorio = grupoRepositorio;
    }

    @GetMapping
    public String listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Integer idGrupo,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {

        var resultado = repositorio.buscar(
                (nombre == null || nombre.isBlank()) ? null : nombre, idGrupo, activo,
                PageRequest.of(pagina, 20, Sort.by("nombre")));

        model.addAttribute("pagina", resultado);
        model.addAttribute("grupos", grupoRepositorio.findAll());
        model.addAttribute("nombre", nombre);
        model.addAttribute("idGrupo", idGrupo);
        model.addAttribute("activo", activo);
        return "portal/alimentos/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        Alimento alimento = new Alimento();
        alimento.setMetodoCarbohidratos(Alimento.MetodoCarbohidratos.MANUAL);
        model.addAttribute("alimento", alimento);
        model.addAttribute("grupos", grupoRepositorio.findAll());
        model.addAttribute("metodos", Alimento.MetodoCarbohidratos.values());
        model.addAttribute("unidades", Alimento.Unidad.values());
        return "portal/alimentos/formulario";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        model.addAttribute("alimento", repositorio.findById(id).orElseThrow());
        model.addAttribute("grupos", grupoRepositorio.findAll());
        model.addAttribute("metodos", Alimento.MetodoCarbohidratos.values());
        model.addAttribute("unidades", Alimento.Unidad.values());
        return "portal/alimentos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Alimento alimento, Model model) {
        // CA-12-02: ningún valor nutricional negativo. Además, la BD exige proteína + grasa + carbohidratos <= 100 g.
        String error = null;
        if (esNegativo(alimento)) {
            error = "Ningún valor nutricional puede ser negativo.";
        } else if (macronutrientesExceden100(alimento)) {
            error = "Proteína + grasa + carbohidratos no pueden superar 100 g por cada 100 g de alimento.";
        }
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("grupos", grupoRepositorio.findAll());
            model.addAttribute("metodos", Alimento.MetodoCarbohidratos.values());
            model.addAttribute("unidades", Alimento.Unidad.values());
            model.addAttribute("alimento", alimento);
            return "portal/alimentos/formulario";
        }
        // Código INS opcional al crear manualmente (CU-12, detalle).
        if (alimento.getCodigoIns() != null && alimento.getCodigoIns().isBlank()) {
            alimento.setCodigoIns(null);
        }
        repositorio.save(alimento);
        // CA-12-03: aviso push CATALOGO — lo dispara el paquete `firebase/` cuando R1 lo tenga listo.
        return "redirect:/portal/alimentos";
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Long id) {
        return cambiarEstado(id, true);
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id) {
        // CA-12-01: "eliminar" desactiva, nunca borra (RN-12).
        return cambiarEstado(id, false);
    }

    private String cambiarEstado(Long id, boolean activo) {
        Alimento alimento = repositorio.findById(id).orElseThrow();
        alimento.setActivo(activo);
        repositorio.save(alimento);
        return "redirect:/portal/alimentos";
    }

    private boolean esNegativo(Alimento a) {
        return esNegativo(a.getEnergiaKcal()) || esNegativo(a.getProteinaG())
                || esNegativo(a.getGrasaG()) || esNegativo(a.getCarbohidratosG())
                || (a.getFibraG() != null && esNegativo(a.getFibraG()));
    }

    private boolean macronutrientesExceden100(Alimento a) {
        java.math.BigDecimal suma = a.getProteinaG().add(a.getGrasaG()).add(a.getCarbohidratosG());
        return suma.compareTo(new java.math.BigDecimal("100")) > 0;
    }

    private boolean esNegativo(java.math.BigDecimal valor) {
        return valor != null && valor.signum() < 0;
    }
}