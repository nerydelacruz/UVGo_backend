package com.UVGgo.backend.extraccion;

import java.util.List;

// Respuesta de Claude. El SDK genera el JSON schema a partir de estos records y devuelve el objeto ya tipado.
// Solo nombre y cantidad: cada campo extra son tokens de salida, los más caros.
public record ProductosExtraidos(List<Producto> productos) {
    public record Producto(String nombre, int cantidad) {}
}
