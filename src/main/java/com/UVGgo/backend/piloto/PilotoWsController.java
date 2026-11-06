package com.UVGgo.backend.piloto;

import java.util.*;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Controller
public class PilotoWsController {
    private final AsignacionService asignaciones;
    public PilotoWsController(AsignacionService asignaciones) { this.asignaciones=asignaciones; }
    /** Al suscribirse a /app/pilotos/{id}/pedidos el piloto recibe, una sola vez, sus pedidos vigentes. */
    @SubscribeMapping("/pilotos/{pilotoId}/pedidos")
    public List<PilotoDtos.Asignacion> pedidos(@DestinationVariable UUID pilotoId) { return asignaciones.pedidosDe(pilotoId); }
}
