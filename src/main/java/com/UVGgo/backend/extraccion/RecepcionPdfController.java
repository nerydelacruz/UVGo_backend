package com.UVGgo.backend.extraccion;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/extracciones")
public class RecepcionPdfController {
    private final RecepcionPdfService servicio;

    public RecepcionPdfController(RecepcionPdfService servicio) {
        this.servicio = servicio;
    }

    @PostMapping(value = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecepcionPdfService.PdfRecibido recibir(@RequestParam("archivo") MultipartFile archivo) {
        return servicio.recibir(archivo);
    }
}
