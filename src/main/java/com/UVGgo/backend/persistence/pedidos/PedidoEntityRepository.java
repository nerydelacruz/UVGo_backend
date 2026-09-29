package com.UVGgo.backend.persistence.pedidos;

import com.UVGgo.backend.domain.pedidos.EstadoPedido;
import com.UVGgo.backend.domain.pedidos.Pedido;
import com.UVGgo.backend.domain.repository.pedidos.PedidoRepository;
import com.UVGgo.backend.persistence.crud.pedidos.PedidoCrudRepository;
import com.UVGgo.backend.persistence.entity.pedidos.PedidoEntity;
import com.UVGgo.backend.persistence.mapper.pedidos.PedidoMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class PedidoEntityRepository implements PedidoRepository {
    private final PedidoCrudRepository crud;
    private final PedidoMapper mapper;

    public PedidoEntityRepository(PedidoCrudRepository crud, PedidoMapper mapper) {
        this.crud = crud;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Pedido agregar(Pedido pedido) {
        PedidoEntity entity = mapper.toEntity(pedido);
        entity.getDetalles().forEach(detalle -> detalle.setPedido(entity));
        return mapper.toPedido(crud.save(entity));
    }

    @Override
    public List<Pedido> getAll() {
        return crud.findAllByOrderByFechaCreacionDescPedidoIdDesc().stream().map(mapper::toPedido).toList();
    }

    @Override
    public Optional<Pedido> getById(int pedidoId) {
        return crud.findById(pedidoId).map(mapper::toPedido);
    }

    @Override
    @Transactional
    public boolean cancelarPendiente(int pedidoId, Instant fechaCancelacion) {
        return crud.cancelarPendiente(pedidoId, fechaCancelacion,
                EstadoPedido.PENDIENTE_COTIZACION, EstadoPedido.CANCELADO) == 1;
    }
}
