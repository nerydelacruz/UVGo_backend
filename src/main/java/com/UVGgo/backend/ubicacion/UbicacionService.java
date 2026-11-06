package com.UVGgo.backend.ubicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** Última posición conocida por pedido, solo en memoria: sirve a quien abre el mapa a mitad del recorrido. */
@Service
public class UbicacionService {
    private final Map<UUID, UbicacionDtos.Ubicacion> ultimas=new ConcurrentHashMap<>();
    private final Clock reloj;
    public UbicacionService() { this(Clock.systemUTC()); }
    UbicacionService(Clock reloj) { this.reloj=reloj; }

    public UbicacionDtos.Ubicacion registrar(UUID pedidoId,UbicacionDtos.Posicion p) {
        var u=new UbicacionDtos.Ubicacion(p.latitud(),p.longitud(),p.rumbo(),Instant.now(reloj));
        ultimas.put(pedidoId,u);
        return u;
    }
    public Optional<UbicacionDtos.Ubicacion> ultima(UUID pedidoId) { return Optional.ofNullable(ultimas.get(pedidoId)); }
}
