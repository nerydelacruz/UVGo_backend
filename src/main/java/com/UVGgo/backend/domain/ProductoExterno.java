package com.UVGgo.backend.domain;

import java.math.BigDecimal;

// url es la ficha del producto en Google Shopping (ahí aparecen las tiendas que lo venden).
// titulo, url, imagen, tienda, precio y moneda coinciden con el producto "web" de una solicitud de cotización.
public record ProductoExterno(String titulo, String url, String imagen, String tienda, BigDecimal precio, String moneda,
                              String descripcion) {
}
