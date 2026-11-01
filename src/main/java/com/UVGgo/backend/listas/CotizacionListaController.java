package com.UVGgo.backend.listas;

import com.UVGgo.backend.pedido.PedidoDtos;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listas-materiales")
public class CotizacionListaController {
    private final CotizacionListaService cotizaciones;
    public CotizacionListaController(CotizacionListaService cotizaciones) { this.cotizaciones=cotizaciones; }

    @PostMapping("/{id}/solicitud")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public PedidoDtos.Detalle solicitar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody PedidoDtos.DatosLista datos) {
        return cotizaciones.solicitar(id,UUID.fromString(jwt.getSubject()),datos);
    }
}
