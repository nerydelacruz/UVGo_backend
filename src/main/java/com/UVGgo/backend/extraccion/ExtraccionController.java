package com.UVGgo.backend.extraccion;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// Bajo /api/v1 para que pida sesión: cada análisis consume créditos de Claude
@RestController
@RequestMapping("/api/v1/extracciones")
public class ExtraccionController {
    private final ExtraccionService extraccion;
    public ExtraccionController(ExtraccionService extraccion) { this.extraccion = extraccion; }

    @PostMapping(value = "/productos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasRole('ESTUDIANTE')")
    public ProductosExtraidos extraer(@RequestParam("archivo") MultipartFile archivo) {
        return extraccion.extraer(archivo);
    }
}
