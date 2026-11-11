package com.UVGgo.backend.piloto;

import com.UVGgo.backend.pedido.PedidoDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@Validated
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class PilotoController {
    private final AsignacionService asignaciones;
    private final PilotoService pilotos;
    public PilotoController(AsignacionService asignaciones,PilotoService pilotos) { this.asignaciones=asignaciones; this.pilotos=pilotos; }
    /** Crea la cuenta de un piloto; después entra por /auth/login como cualquier usuario. */
    @PostMapping("/pilotos") @ResponseStatus(HttpStatus.CREATED)
    public PilotoDtos.Resumen registrar(@Valid @RequestBody PilotoDtos.Alta r) { return pilotos.registrar(r); }
    /** Pilotos que se pueden asignar a un pedido. */
    @GetMapping("/pilotos")
    public List<PilotoDtos.Resumen> disponibles() { return asignaciones.disponibles(); }
    /** Asigna (o reasigna) el piloto del pedido; el piloto lo recibe al instante por WebSocket. */
    @PostMapping("/pedidos/{pedidoId}/piloto")
    public PedidoDtos.Detalle asignar(@PathVariable UUID pedidoId,@Valid @RequestBody PilotoDtos.Asignar r) {
        return asignaciones.asignar(pedidoId,r);
    }
    @DeleteMapping("/pedidos/{pedidoId}/piloto")
    public PedidoDtos.Detalle quitar(@PathVariable UUID pedidoId,@RequestParam @Min(0) long version) {
        return asignaciones.quitar(pedidoId,version);
    }
}
