package com.UVGgo.backend.extraccion;

import java.math.BigDecimal;
import java.util.List;

/** Borrador independiente de Articulo: los datos desconocidos pueden ser null. */
public record MaterialExtraido(String nombre, String descripcion, BigDecimal cantidad,
        String unidad, String observaciones, String referencia, List<String> advertencias) {}
