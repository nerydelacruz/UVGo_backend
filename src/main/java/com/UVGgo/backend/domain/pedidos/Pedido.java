package com.UVGgo.backend.domain.pedidos;

import java.time.Instant;
import java.util.List;

public record Pedido(Integer pedidoId, EstadoPedido estado, Instant fechaCreacion,
                     Instant fechaCancelacion, List<PedidoDetalle> detalles) {
    public Pedido {
        detalles = List.copyOf(detalles);
    }
}
