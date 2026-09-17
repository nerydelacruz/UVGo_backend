package com.UVGgo.backend.pedido;

import com.UVGgo.backend.api.ApiException;
import com.UVGgo.backend.usuario.Estudiante;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name="pedidos")
public class Pedido {
    @Id private UUID id=UUID.randomUUID();
    @Version private long version;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="estudiante_id") private Estudiante estudiante;
    @Column(nullable=false, length=150) private String curso;
    @Column(nullable=false, length=150) private String nombreCatedratico;
    @Column(nullable=false) private LocalDate fechaLimite;
    @Column(nullable=false) private Instant fechaSolicitud=Instant.now();
    @Column(nullable=false) private Instant fechaActualizacion=Instant.now();
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private EstadoPedido estado=EstadoPedido.SOLICITADO;
    @Column(length=2000) private String observaciones;
    @Column(length=150) private String puntoEntregaPreferido;
    @OneToMany(mappedBy="pedido", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderColumn(name="posicion") private List<ItemPedido> items=new ArrayList<>();
    protected Pedido() {}
    public Pedido(Estudiante estudiante, PedidoDtos.Solicitud s) { this.estudiante=estudiante; actualizar(s); }
    public void actualizar(PedidoDtos.Solicitud s) {
        if (estado!=EstadoPedido.SOLICITADO) throw ApiException.conflicto("Solo se puede editar antes de cotizar");
        curso=s.curso().strip(); nombreCatedratico=s.nombreCatedratico().strip(); fechaLimite=s.fechaLimite();
        observaciones=s.observaciones(); puntoEntregaPreferido=s.puntoEntregaPreferido();
        fechaActualizacion=Instant.now();
        items.clear(); s.materiales().forEach(m -> items.add(new ItemPedido(this,m)));
    }
    public void cancelar() {
        if (estado==EstadoPedido.CANCELADO) return;
        if (estado!=EstadoPedido.SOLICITADO && estado!=EstadoPedido.COTIZADO && estado!=EstadoPedido.CONFIRMADO)
            throw ApiException.conflicto("No se puede cancelar desde el inicio de la compra");
        estado=EstadoPedido.CANCELADO;
        fechaActualizacion=Instant.now();
    }
    public UUID getId() { return id; }
    public long getVersion() { return version; }
    public Estudiante getEstudiante() { return estudiante; }
    public String getCurso() { return curso; }
    public String getNombreCatedratico() { return nombreCatedratico; }
    public LocalDate getFechaLimite() { return fechaLimite; }
    public Instant getFechaSolicitud() { return fechaSolicitud; }
    public EstadoPedido getEstado() { return estado; }
    public String getObservaciones() { return observaciones; }
    public String getPuntoEntregaPreferido() { return puntoEntregaPreferido; }
    public List<ItemPedido> getItems() { return Collections.unmodifiableList(items); }
}
