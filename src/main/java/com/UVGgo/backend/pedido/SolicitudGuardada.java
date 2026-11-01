package com.UVGgo.backend.pedido;

import java.util.List;
import java.util.UUID;

// Se publica al crear o editar una solicitud con materiales de texto libre, para buscarlos en el catálogo en segundo plano.
record SolicitudGuardada(UUID pedidoId, List<Material> materiales) {
    // codigo y termino los sugiere Claude al leer el PDF; pueden venir vacíos
    record Material(UUID itemId, String texto, String codigo, String termino) {}
}
