package com.UVGgo.backend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record Articulo(
        @JsonProperty(access = JsonProperty.Access.READ_ONLY) Integer id,
        @NotBlank @Size(max = 300) String nombre,
        @NotBlank @Size(max = 4000) String descripcion,
        @Size(max = 100) String categoria,
        @Size(max = 2000) String imagen,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precio,
        @Size(max = 10) String moneda,
        @Size(max = 150) String tienda,
        @Size(max = 2000) String url,
        @Size(max = 255) String idExterno) {
}
