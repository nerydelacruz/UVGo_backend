package com.UVGgo.backend.domain.repository.pedidos;

import com.UVGgo.backend.domain.pedidos.EstadoPedido;
import com.UVGgo.backend.domain.pedidos.Pedido;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository {
    Pedido agregar(Pedido pedido);
    List<Pedido> getAll();
    Optional<Pedido> getById(int pedidoId);
    boolean cancelarPendiente(int pedidoId, Instant fechaCancelacion);
    boolean actualizarEstado(int pedidoId, EstadoPedido actual, EstadoPedido nuevo);
    boolean contieneArticulo(int articuloId);
}
