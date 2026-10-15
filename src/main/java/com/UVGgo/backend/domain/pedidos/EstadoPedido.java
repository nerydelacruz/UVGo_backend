package com.UVGgo.backend.domain.pedidos;

public enum EstadoPedido {
    SOLICITADO,
    REVISION,
    COTIZADO,
    CONFIRMADO,
    EN_RUTA,
    ENTREGADO,
    CANCELADO;

    // Flujo de seguimiento: cada estado solo avanza al siguiente.
    // ENTREGADO y CANCELADO son finales (null); la cancelación tiene su propia ruta.
    public EstadoPedido siguiente() {
        return switch (this) {
            case SOLICITADO -> REVISION;
            case REVISION -> COTIZADO;
            case COTIZADO -> CONFIRMADO;
            case CONFIRMADO -> EN_RUTA;
            case EN_RUTA -> ENTREGADO;
            case ENTREGADO, CANCELADO -> null;
        };
    }

    // Retroceso de un paso para corregir errores. SOLICITADO es el inicio y CANCELADO no se revierte (null).
    public EstadoPedido anterior() {
        return switch (this) {
            case REVISION -> SOLICITADO;
            case COTIZADO -> REVISION;
            case CONFIRMADO -> COTIZADO;
            case EN_RUTA -> CONFIRMADO;
            case ENTREGADO -> EN_RUTA;
            case SOLICITADO, CANCELADO -> null;
        };
    }
}
