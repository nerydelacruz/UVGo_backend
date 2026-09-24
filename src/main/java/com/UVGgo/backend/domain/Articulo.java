package com.UVGgo.backend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public record Articulo(
        @JsonProperty(access = JsonProperty.Access.READ_ONLY) Integer id,
        @JsonProperty(access = JsonProperty.Access.READ_ONLY) Integer kitId,
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Size(max = 500) String descripcion,
        @NotNull @DecimalMin("0.001") @Digits(integer = 9, fraction = 3) BigDecimal cantidad,
        @Size(max = 100) String categoria,
        @JsonProperty(access = JsonProperty.Access.READ_ONLY) Instant fechaCreacion,
        @Size(max = 2000) String observacionesCotizacion) {
}
