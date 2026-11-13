package com.UVGgo.backend.reporte;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reportes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ReporteController {
    private final ReporteService reportes;
    public ReporteController(ReporteService reportes) { this.reportes=reportes; }

    /** Reporte entre dos fechas (yyyy-mm-dd, ambas incluidas). Sin fechas: los últimos 30 días. */
    @GetMapping
    public ReporteDtos.Reporte generar(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
                                       @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportes.generar(desde,hasta);
    }
}
