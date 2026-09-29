package com.UVGgo.backend.persistence.mapper.pedidos;

import com.UVGgo.backend.domain.pedidos.Pedido;
import com.UVGgo.backend.domain.pedidos.PedidoDetalle;
import com.UVGgo.backend.persistence.entity.pedidos.PedidoDetalleEntity;
import com.UVGgo.backend.persistence.entity.pedidos.PedidoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PedidoMapper {
    Pedido toPedido(PedidoEntity entity);
    PedidoDetalle toDetalle(PedidoDetalleEntity entity);

    @Mapping(target = "pedidoId", ignore = true)
    PedidoEntity toEntity(Pedido pedido);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pedido", ignore = true)
    PedidoDetalleEntity toDetalleEntity(PedidoDetalle detalle);
}
