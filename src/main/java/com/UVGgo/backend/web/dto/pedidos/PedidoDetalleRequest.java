package com.UVGgo.backend.web.dto.pedidos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PedidoDetalleRequest(
        @NotNull @Positive Integer kitId,
        @NotNull @Positive Integer kitArticuloId,
        @NotNull @DecimalMin("0.001") @Digits(integer = 9, fraction = 3) BigDecimal cantidad) {
}
