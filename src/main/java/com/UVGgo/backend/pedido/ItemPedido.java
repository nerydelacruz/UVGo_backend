package com.UVGgo.backend.pedido;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name="items_pedido")
public class ItemPedido {
    @Id private UUID id=UUID.randomUUID();
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="pedido_id") private Pedido pedido;
    @Column(nullable=false, length=500) private String descripcion;
    @Column(nullable=false, precision=12, scale=3) private BigDecimal cantidad;
    @Column(nullable=false, length=40) private String unidadMedida;
    protected ItemPedido() {}
    ItemPedido(Pedido pedido, PedidoDtos.Material m) {
        this.pedido=pedido; this.descripcion=m.descripcion().strip(); this.cantidad=m.cantidad(); this.unidadMedida=m.unidadMedida().strip();
    }
    public UUID getId() { return id; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getCantidad() { return cantidad; }
    public String getUnidadMedida() { return unidadMedida; }
}
