package com.UVGgo.backend.domain.pedidos;

// Versión provisional: David no subió el archivo original; se reconstruyó a partir de PedidoService y sus pruebas.
public record SeguimientoPedido(Integer pedidoId, EstadoPedido estado,
                                EstadoPedido siguienteEstado, EstadoPedido anteriorEstado) {
}
