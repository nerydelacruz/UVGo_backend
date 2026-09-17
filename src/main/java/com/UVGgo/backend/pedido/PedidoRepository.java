package com.UVGgo.backend.pedido;

import org.springframework.data.jpa.repository.*;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido,UUID>, JpaSpecificationExecutor<Pedido> {}
