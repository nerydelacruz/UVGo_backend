package com.UVGgo.backend.piloto;

import com.UVGgo.backend.pedido.Pedido;
import com.UVGgo.backend.pedido.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/** Qué puede hacer un piloto ya autenticado (con su JWT) en el WebSocket. */
@Service
@Transactional(readOnly=true)
public class AccesoPiloto {
    private final PedidoRepository pedidos;
    public AccesoPiloto(PedidoRepository pedidos) { this.pedidos=pedidos; }
    /** ¿Es este pedido, todavía por entregar, del piloto? */
    public boolean asignado(UUID pilotoId,UUID pedidoId) {
        return pedidos.existsByIdAndPiloto_IdAndEstadoIn(pedidoId,pilotoId,Pedido.ESTADOS_ENTREGABLES);
    }
}
