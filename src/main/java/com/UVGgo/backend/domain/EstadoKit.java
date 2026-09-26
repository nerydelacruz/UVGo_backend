package com.UVGgo.backend.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

// El orden importa: el ordinal de cada valor (BASE=0, PERSONALIZADO=1) es el
// código que se guarda en la base y el que se expone en el JSON. Los nombres
// se consultan en la tabla estados_kit.
@JsonFormat(shape = JsonFormat.Shape.NUMBER)
public enum EstadoKit {
    BASE, PERSONALIZADO
}
