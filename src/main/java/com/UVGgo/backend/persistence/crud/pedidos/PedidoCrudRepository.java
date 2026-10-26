package com.UVGgo.backend.persistence.crud.pedidos;

import com.UVGgo.backend.domain.pedidos.EstadoPedido;
import com.UVGgo.backend.persistence.entity.pedidos.PedidoEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PedidoCrudRepository extends CrudRepository<PedidoEntity, Integer> {
    List<PedidoEntity> findAllByOrderByFechaCreacionDescPedidoIdDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PedidoEntity p set p.estado = :cancelado, p.fechaCancelacion = :fecha
            where p.pedidoId = :id and p.estado = :pendiente
            """)
    int cancelarPendiente(@Param("id") int pedidoId, @Param("fecha") Instant fecha,
                          @Param("pendiente") EstadoPedido pendiente, @Param("cancelado") EstadoPedido cancelado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PedidoEntity p set p.estado = :nuevo
            where p.pedidoId = :id and p.estado = :actual
            """)
    int actualizarEstado(@Param("id") int pedidoId, @Param("actual") EstadoPedido actual,
                         @Param("nuevo") EstadoPedido nuevo);

    @Query("select count(d) > 0 from PedidoDetalleEntity d where d.articuloId = :articuloId")
    boolean contieneArticulo(@Param("articuloId") int articuloId);
}
