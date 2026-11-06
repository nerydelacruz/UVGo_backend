package com.UVGgo.backend.ubicacion;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/** Recibe en /app/pedidos/{id}/ubicacion y retransmite a /topic/pedidos/{id}/ubicacion. */
@Controller
public class UbicacionWsController {
    private final UbicacionService ubicaciones;
    private final SimpMessagingTemplate mensajes;
    public UbicacionWsController(UbicacionService ubicaciones,SimpMessagingTemplate mensajes) {
        this.ubicaciones=ubicaciones; this.mensajes=mensajes;
    }
    @MessageMapping("/pedidos/{pedidoId}/ubicacion")
    public void publicar(@DestinationVariable UUID pedidoId,@Valid UbicacionDtos.Posicion posicion) {
        mensajes.convertAndSend("/topic/pedidos/"+pedidoId+"/ubicacion",ubicaciones.registrar(pedidoId,posicion));
    }
}
