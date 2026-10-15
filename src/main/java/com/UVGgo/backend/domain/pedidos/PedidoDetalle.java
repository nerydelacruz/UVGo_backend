package com.UVGgo.backend.domain.pedidos;

import java.math.BigDecimal;

// articuloId corresponde al artículo seleccionado del stock.
public record PedidoDetalle(Integer id, Integer articuloId, BigDecimal cantidad) {
}
