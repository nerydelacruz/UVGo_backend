package com.UVGgo.backend.domain.service.pedidos;

import com.UVGgo.backend.domain.pedidos.EstadoPedido;
import com.UVGgo.backend.domain.pedidos.Pedido;
import com.UVGgo.backend.domain.pedidos.PedidoDetalle;
import com.UVGgo.backend.domain.pedidos.SeguimientoPedido;
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
        var articulos = new HashSet<Integer>();
        for (PedidoDetalle detalle : detalles) {
            if (!articulos.add(detalle.articuloId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se puede repetir un artículo en el pedido");
            }
        }
        // Se registran los artículos declarados, sin consultar ni modificar el stock.
        var nuevos = detalles.stream()
                .map(d -> new PedidoDetalle(null, d.articuloId(), d.cantidad()))
                .toList();
        return repository.agregar(new Pedido(null, EstadoPedido.SOLICITADO,
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
        if (pedido.estado() != EstadoPedido.SOLICITADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden cancelar pedidos en estado SOLICITADO");
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

    public SeguimientoPedido consultarSeguimiento(int pedidoId) {
        Pedido pedido = consultar(pedidoId);
        return new SeguimientoPedido(pedido.pedidoId(), pedido.estado(),
                pedido.estado().siguiente(), pedido.estado().anterior());
    }

    @Transactional
    public Pedido actualizarEstado(int pedidoId, EstadoPedido nuevo) {
        if (nuevo == EstadoPedido.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para cancelar use POST /pedidos/{pedidoId}/cancelar");
        }
        Pedido pedido = consultar(pedidoId);
        // Repetir la misma actualización (doble clic, reintento) devuelve el pedido sin cambios.
        if (pedido.estado() == nuevo) {
            return pedido;
        }
        // Solo se mueve un paso: al siguiente estado o al anterior.
        if (nuevo != pedido.estado().siguiente() && nuevo != pedido.estado().anterior()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido está en " + pedido.estado()
                    + "; siguiente permitido: " + pedido.estado().siguiente()
                    + ", anterior permitido: " + pedido.estado().anterior());
        }
        // Igual que en cancelar: la condición se comprueba en la base para no sobrescribir otro cambio.
        if (!repository.actualizarEstado(pedidoId, pedido.estado(), nuevo)) {
            Pedido actual = consultar(pedidoId);
            if (actual.estado() == nuevo) {
                return actual;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estado del pedido ha cambiado");
        }
        return consultar(pedidoId);
    }
}
