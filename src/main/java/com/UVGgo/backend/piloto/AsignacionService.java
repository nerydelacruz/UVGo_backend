package com.UVGgo.backend.piloto;

import com.UVGgo.backend.api.ApiException;
import com.UVGgo.backend.pedido.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class AsignacionService {
    private final PedidoRepository pedidos;
    private final PilotoRepository pilotos;
    private final ApplicationEventPublisher eventos;
    public AsignacionService(PedidoRepository pedidos,PilotoRepository pilotos,ApplicationEventPublisher eventos) {
        this.pedidos=pedidos; this.pilotos=pilotos; this.eventos=eventos;
    }
    @Transactional(readOnly=true)
    public List<PilotoDtos.Resumen> disponibles() { return pilotos.findByActivoTrueOrderByNombre().stream().map(PilotoDtos.Resumen::de).toList(); }

    public PedidoDtos.Detalle asignar(UUID pedidoId,PilotoDtos.Asignar r) {
        Pedido p=pedidos.findById(pedidoId).orElseThrow(ApiException::noEncontrado); comprobarVersion(p,r.version());
        Piloto nuevo=pilotos.findById(r.pilotoId()).filter(Piloto::isActivo).orElseThrow(ApiException::noEncontrado);
        Piloto anterior=p.getPiloto();
        if (anterior!=null && anterior.getId().equals(nuevo.getId())) return PedidoDtos.Detalle.de(p);
        p.asignarPiloto(nuevo); pedidos.flush();
        if (anterior!=null) eventos.publishEvent(new CambioAsignacion(anterior.getId(),mensaje(p,false)));
        eventos.publishEvent(new CambioAsignacion(nuevo.getId(),mensaje(p,true)));
        return PedidoDtos.Detalle.de(p);
    }
    public PedidoDtos.Detalle quitar(UUID pedidoId,long version) {
        Pedido p=pedidos.findById(pedidoId).orElseThrow(ApiException::noEncontrado); comprobarVersion(p,version);
        Piloto anterior=p.getPiloto();
        if (anterior==null) return PedidoDtos.Detalle.de(p);
        p.quitarPiloto(); pedidos.flush();
        eventos.publishEvent(new CambioAsignacion(anterior.getId(),mensaje(p,false)));
        return PedidoDtos.Detalle.de(p);
    }
    /** Pedidos que el piloto tiene por entregar ahora; es lo primero que recibe al conectarse. */
    @Transactional(readOnly=true)
    public List<PilotoDtos.Asignacion> pedidosDe(UUID pilotoId) {
        return pedidos.findByPiloto_IdAndEstadoIn(pilotoId,Pedido.ESTADOS_ENTREGABLES).stream()
                .sorted(Comparator.comparing(Pedido::getFechaActualizacion).reversed()).map(p -> mensaje(p,true)).toList();
    }
    private PilotoDtos.Asignacion mensaje(Pedido p,boolean asignado) {
        return new PilotoDtos.Asignacion(p.getId(),p.getCurso(),p.getPuntoEntregaPreferido(),p.getEstado(),asignado,Instant.now());
    }
    private void comprobarVersion(Pedido p,long version) {
        if (p.getVersion()!=version) throw ApiException.conflicto("El pedido cambió; consulte su versión actual");
    }
}
