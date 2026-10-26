package com.UVGgo.backend.correo;

// Se publica como evento; CorreoService lo envía cuando la transacción que lo generó se confirma.
public record Correo(String para, String asunto, String texto, String html) {}
