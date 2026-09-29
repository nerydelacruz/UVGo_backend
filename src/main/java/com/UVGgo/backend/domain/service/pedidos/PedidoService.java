package com.UVGgo.backend.domain.service.pedidos;

import com.UVGgo.backend.domain.pedidos.EstadoPedido;
import com.UVGgo.backend.domain.pedidos.Pedido;
import com.UVGgo.backend.domain.pedidos.PedidoDetalle;
import com.UVGgo.backend.domain.repository.pedidos.PedidoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

@Service
public class PedidoService {
    private final PedidoRepository repository;

    public PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Pedido crear(List<PedidoDetalle> detalles) {
        var referencias = new HashSet<Referencia>();
        for (PedidoDetalle detalle : detalles) {
            if (!referencias.add(new Referencia(detalle.kitId(), detalle.kitArticuloId()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se puede repetir un artículo del mismo kit en el pedido");
            }
        }
        // Se registran referencias declaradas, sin consultar ni modificar kits.
        var nuevos = detalles.stream()
                .map(d -> new PedidoDetalle(null, d.kitId(), d.kitArticuloId(), d.cantidad()))
                .toList();
        return repository.agregar(new Pedido(null, EstadoPedido.PENDIENTE_COTIZACION,
                Instant.now(), null, nuevos));
    }

    public List<Pedido> listar() {
        return repository.getAll();
    }

    public Pedido consultar(int pedidoId) {
        return repository.getById(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    @Transactional
    public Pedido cancelar(int pedidoId) {
        Pedido pedido = consultar(pedidoId);
        if (pedido.estado() == EstadoPedido.CANCELADO) {
            return pedido;
        }
        if (pedido.estado() != EstadoPedido.PENDIENTE_COTIZACION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden cancelar pedidos pendientes");
        }
        // La condición se comprueba también en la base para no sobrescribir otro estado.
        if (!repository.cancelarPendiente(pedidoId, Instant.now())) {
            Pedido actual = consultar(pedidoId);
            if (actual.estado() == EstadoPedido.CANCELADO) {
                return actual;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estado del pedido ha cambiado");
        }
        return consultar(pedidoId);
    }

    private record Referencia(Integer kitId, Integer kitArticuloId) {
    }
}
