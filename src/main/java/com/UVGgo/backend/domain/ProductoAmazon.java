package com.UVGgo.backend.domain;

import java.math.BigDecimal;

public record ProductoAmazon(
        String asin,
        String titulo,
        String imagenUrl,
        String url,
        BigDecimal precio,
        String moneda) {
}
