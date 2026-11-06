package com.UVGgo.backend.ubicacion;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pedidos")
public class UbicacionController {
    private final UbicacionService ubicaciones;
    public UbicacionController(UbicacionService ubicaciones) { this.ubicaciones=ubicaciones; }
    /** Última posición conocida del repartidor; 204 si todavía no ha publicado ninguna. */
    @GetMapping("/{pedidoId}/ubicacion") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UbicacionDtos.Ubicacion> ultima(@PathVariable UUID pedidoId) {
        return ubicaciones.ultima(pedidoId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
}
