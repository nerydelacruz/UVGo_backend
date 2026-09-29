package com.UVGgo.backend.persistence.entity.pedidos;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "pedido_detalles")
public class PedidoDetalleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoEntity pedido;

    @Column(name = "kit_id", nullable = false)
    private Integer kitId;

    @Column(name = "kit_articulo_id", nullable = false)
    private Integer kitArticuloId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public PedidoEntity getPedido() { return pedido; }
    public void setPedido(PedidoEntity pedido) { this.pedido = pedido; }
    public Integer getKitId() { return kitId; }
    public void setKitId(Integer kitId) { this.kitId = kitId; }
    public Integer getKitArticuloId() { return kitArticuloId; }
    public void setKitArticuloId(Integer kitArticuloId) { this.kitArticuloId = kitArticuloId; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
}
