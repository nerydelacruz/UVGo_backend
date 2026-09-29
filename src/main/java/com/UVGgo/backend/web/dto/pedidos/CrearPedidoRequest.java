package com.UVGgo.backend.web.dto.pedidos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CrearPedidoRequest(@NotEmpty List<@NotNull @Valid PedidoDetalleRequest> detalles) {
}
