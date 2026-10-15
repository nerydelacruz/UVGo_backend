package com.UVGgo.backend.web.dto.pedidos;

import com.UVGgo.backend.domain.pedidos.EstadoPedido;
import jakarta.validation.constraints.NotNull;

// Versión provisional: David no subió el archivo original; se reconstruyó a partir de PedidoController y sus pruebas.
public record ActualizarEstadoRequest(@NotNull EstadoPedido estado) {
}
