package com.UVGgo.backend.domain.pedidos;

import java.math.BigDecimal;

// kitArticuloId corresponde a la relación kit-artículo expuesta por la API de kits.
public record PedidoDetalle(Integer id, Integer kitId, Integer kitArticuloId, BigDecimal cantidad) {
}
