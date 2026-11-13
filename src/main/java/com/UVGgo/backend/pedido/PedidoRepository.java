package com.UVGgo.backend.pedido;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public interface PedidoRepository extends JpaRepository<Pedido,UUID>, JpaSpecificationExecutor<Pedido> {
    List<Pedido> findByPiloto_IdAndEstadoIn(UUID pilotoId,Collection<EstadoPedido> estados);
    boolean existsByIdAndPiloto_IdAndEstadoIn(UUID id,UUID pilotoId,Collection<EstadoPedido> estados);

    /**
     * Suma un tramo recorrido (kilómetros y segundos) y sube la velocidad máxima si hace falta, sin tocar la versión:
     * el GPS publica cada pocos segundos y no debe causar 409 al administrador.
     */
    @Transactional
    @Modifying
    @Query(value="UPDATE solicitudes SET kilometros = kilometros + :km, segundos_en_ruta = segundos_en_ruta + :segundos, "
            +"velocidad_maxima = GREATEST(COALESCE(velocidad_maxima, 0), :velocidad) WHERE id = :id", nativeQuery=true)
    int registrarTramo(@Param("id") UUID id,@Param("km") BigDecimal km,@Param("segundos") long segundos,
                       @Param("velocidad") BigDecimal velocidad);

    /** Solicitudes del periodo con todo lo que usa el reporte, en una sola consulta. */
    @Query("select distinct p from Pedido p join fetch p.estudiante left join fetch p.piloto left join fetch p.items "
            +"where p.fechaSolicitud >= :desde and p.fechaSolicitud < :hasta")
    List<Pedido> findParaReporte(@Param("desde") Instant desde,@Param("hasta") Instant hasta);
}
