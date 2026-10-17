package com.nutrimeta.backend.controller;

import com.nutrimeta.backend.service.CatalogoDto;
import com.nutrimeta.backend.service.CatalogoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class CatalogoApiController {

    private final CatalogoService servicio;

    public CatalogoApiController(CatalogoService servicio) {
        this.servicio = servicio;
    }

    // GET /api/catalogo                       -> catálogo completo
    // GET /api/catalogo?desde=2026-10-17T16:14:22.123456 -> solo alimentos modificados después de esa marca
    // La app guarda `sincronizadoEn` de la respuesta y la envía como `desde` la próxima vez.
    @GetMapping("/catalogo")
    public CatalogoDto catalogo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde) {
        return servicio.obtener(desde);
    }
}
