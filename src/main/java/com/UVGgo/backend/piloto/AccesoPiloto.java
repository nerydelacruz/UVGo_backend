package com.UVGgo.backend.piloto;

import com.UVGgo.backend.pedido.Pedido;
import com.UVGgo.backend.pedido.PedidoRepository;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Credencial del piloto/simulador para el WebSocket: una clave compartida (SIMULADOR_CLAVE) más el id del piloto.
 * Sin clave configurada (o de menos de 16 caracteres) el acceso de pilotos queda deshabilitado.
 */
@Service
@Transactional(readOnly=true)
public class AccesoPiloto {
    private static final Logger log=LoggerFactory.getLogger(AccesoPiloto.class);
    static final int CLAVE_MINIMA=16;
    private final PilotoRepository pilotos;
    private final PedidoRepository pedidos;
    private final byte[] clave;
    public AccesoPiloto(PilotoRepository pilotos,PedidoRepository pedidos,
                        @Value("${uvgo.simulador.clave:${SIMULADOR_CLAVE:}}") String clave) {
        this.pilotos=pilotos; this.pedidos=pedidos;
        this.clave=clave.length()>=CLAVE_MINIMA ? clave.getBytes(StandardCharsets.UTF_8) : null;
        if (this.clave==null) log.warn("Acceso de pilotos deshabilitado: SIMULADOR_CLAVE falta o tiene menos de {} caracteres",CLAVE_MINIMA);
    }
    /** El id del piloto si la clave es correcta y el piloto existe y está activo. */
    public Optional<UUID> autenticar(String pilotoId,String claveRecibida) {
        if (clave==null || pilotoId==null || claveRecibida==null) return Optional.empty();
        // Comparación en tiempo constante: no revela cuántos caracteres coinciden
        boolean coincide=MessageDigest.isEqual(clave,claveRecibida.getBytes(StandardCharsets.UTF_8));
        try {
            UUID id=UUID.fromString(pilotoId);
            return coincide && pilotos.findById(id).filter(Piloto::isActivo).isPresent() ? Optional.of(id) : Optional.empty();
        } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
    /** ¿Es este pedido, todavía por entregar, del piloto? */
    public boolean asignado(UUID pilotoId,UUID pedidoId) {
        return pedidos.existsByIdAndPiloto_IdAndEstadoIn(pedidoId,pilotoId,Pedido.ESTADOS_ENTREGABLES);
    }
}
