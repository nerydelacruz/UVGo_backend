package com.UVGgo.backend.pedido;

import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface PedidoRepository extends JpaRepository<Pedido,UUID>, JpaSpecificationExecutor<Pedido> {
    List<Pedido> findByPiloto_IdAndEstadoIn(UUID pilotoId,Collection<EstadoPedido> estados);
    boolean existsByIdAndPiloto_IdAndEstadoIn(UUID id,UUID pilotoId,Collection<EstadoPedido> estados);
}
