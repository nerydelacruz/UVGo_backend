package com.UVGgo.backend.persistence.entity.pedidos;

import jakarta.persistence.*;
import com.UVGgo.backend.domain.pedidos.EstadoPedido;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class PedidoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EstadoPedido estado = EstadoPedido.SOLICITADO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_cancelacion")
    private Instant fechaCancelacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<PedidoDetalleEntity> detalles = new ArrayList<>();

    public Integer getPedidoId() { return pedidoId; }
    public void setPedidoId(Integer pedidoId) { this.pedidoId = pedidoId; }
    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public Instant getFechaCancelacion() { return fechaCancelacion; }
    public void setFechaCancelacion(Instant fechaCancelacion) { this.fechaCancelacion = fechaCancelacion; }
    public List<PedidoDetalleEntity> getDetalles() { return detalles; }
    public void setDetalles(List<PedidoDetalleEntity> detalles) { this.detalles = detalles; }
}
