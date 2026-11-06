package com.UVGgo.backend.piloto;

import com.UVGgo.backend.pedido.EstadoPedido;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class PilotoDtos {
    private PilotoDtos() {}
    public record Resumen(UUID id, String nombre) {
        static Resumen de(Piloto p) { return new Resumen(p.getId(),p.getNombre()); }
    }
    public record Asignar(@NotNull @PositiveOrZero Long version, @NotNull UUID pilotoId) {}
    /**
     * Lo que recibe el piloto (simulador) por WebSocket. asignado=false avisa que el pedido ya no es suyo.
     * destino es el punto de entrega que pidió el estudiante; puede ser null.
     */
    public record Asignacion(UUID pedidoId, String curso, String destino, EstadoPedido estado, boolean asignado, Instant marca) {}
}
