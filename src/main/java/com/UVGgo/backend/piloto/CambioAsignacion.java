package com.UVGgo.backend.piloto;

import java.util.UUID;

/** Evento interno: a quién avisar (por WebSocket, ya confirmada la transacción) de un cambio de asignación. */
public record CambioAsignacion(UUID pilotoId, PilotoDtos.Asignacion asignacion) {}
