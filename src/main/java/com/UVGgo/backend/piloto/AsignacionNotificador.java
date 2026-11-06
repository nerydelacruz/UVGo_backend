package com.UVGgo.backend.piloto;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Avisa al piloto en /topic/pilotos/{id}/asignaciones cuando ya se confirmó el cambio en la base. */
@Component
public class AsignacionNotificador {
    private final SimpMessagingTemplate mensajes;
    public AsignacionNotificador(SimpMessagingTemplate mensajes) { this.mensajes=mensajes; }
    @TransactionalEventListener(fallbackExecution=true)
    public void avisar(CambioAsignacion cambio) {
        mensajes.convertAndSend("/topic/pilotos/"+cambio.pilotoId()+"/asignaciones",cambio.asignacion());
    }
}
