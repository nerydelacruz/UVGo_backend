package com.UVGgo.backend.pedido;

import com.UVGgo.backend.api.ApiException;
import com.UVGgo.backend.domain.Articulo;
import com.UVGgo.backend.usuario.Estudiante;
import jakarta.persistence.*;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name="solicitudes")
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
    @Column(precision=12, scale=2) private BigDecimal cargoServicio;
    @Column(length=2000) private String notasCotizacion;
    private Instant fechaCotizacion;
    private Instant fechaConfirmacion;
    @Column(length=300) private String comprobanteRuta;
    @Column(length=255) private String comprobanteNombre;
    @Column(length=100) private String comprobanteTipo;
    private Long comprobanteTamano;
    private Instant fechaComprobante;
    private Instant fechaPago;
    @OneToMany(mappedBy="pedido", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderColumn(name="posicion") private List<ItemPedido> items=new ArrayList<>();
    protected Pedido() {}
    // catalogo: artículos referenciados por la solicitud, ya validados por el servicio.
    public Pedido(Estudiante estudiante, PedidoDtos.Solicitud s, Map<Integer,Articulo> catalogo) {
        this.estudiante=estudiante; actualizar(s,catalogo);
    }
    public void actualizar(PedidoDtos.Solicitud s, Map<Integer,Articulo> catalogo) {
        if (estado!=EstadoPedido.SOLICITADO) throw ApiException.conflicto("Solo se puede editar antes de cotizar");
        curso=s.curso().strip(); nombreCatedratico=s.nombreCatedratico().strip(); fechaLimite=s.fechaLimite();
        observaciones=s.observaciones(); puntoEntregaPreferido=s.puntoEntregaPreferido();
        fechaActualizacion=Instant.now();
        items.clear();
        s.productos().forEach(pr -> items.add(pr.articuloId()!=null
                ? new ItemPedido(this,catalogo.get(pr.articuloId()),pr.cantidad())
                : new ItemPedido(this,pr.web(),pr.cantidad())));
    }
    // El administrador pone precio a cada ítem; puede corregir la cotización mientras el estudiante no la acepte.
    public void cotizar(Map<UUID,BigDecimal> precios, BigDecimal cargoServicio, String notas) {
        if (estado!=EstadoPedido.SOLICITADO && estado!=EstadoPedido.COTIZADO)
            throw ApiException.conflicto("Solo se cotizan solicitudes pendientes o ya cotizadas");
        Set<UUID> ids=new HashSet<>(); items.forEach(i -> ids.add(i.getId()));
        if (!ids.equals(precios.keySet()))
            throw new ApiException(HttpStatus.BAD_REQUEST,"Debe indicar un precio para cada producto de la solicitud");
        items.forEach(i -> i.cotizar(precios.get(i.getId())));
        this.cargoServicio=cargoServicio; this.notasCotizacion=notas;
        estado=EstadoPedido.COTIZADO;
        fechaCotizacion=fechaActualizacion=Instant.now();
    }
    // Al aceptar la cotización, la solicitud queda confirmada como pedido.
    public void aceptar() {
        if (estado!=EstadoPedido.COTIZADO) throw ApiException.conflicto("Solo se puede aceptar una solicitud cotizada");
        estado=EstadoPedido.CONFIRMADO;
        fechaConfirmacion=fechaActualizacion=Instant.now();
    }
    // El estudiante sube (o reemplaza) el comprobante mientras el pedido confirmado no se marque como pagado.
    // Devuelve la ruta del comprobante anterior para borrarlo del almacenamiento, o null.
    public String adjuntarComprobante(String ruta, String nombre, String tipo, long tamano) {
        if (estado!=EstadoPedido.CONFIRMADO) throw ApiException.conflicto("Solo se adjunta el comprobante de un pedido confirmado pendiente de pago");
        String anterior=comprobanteRuta;
        comprobanteRuta=ruta; comprobanteNombre=nombre; comprobanteTipo=tipo; comprobanteTamano=tamano;
        fechaComprobante=fechaActualizacion=Instant.now();
        return anterior;
    }
    // El administrador revisa el comprobante y registra el pago.
    public void registrarPago() {
        if (estado!=EstadoPedido.CONFIRMADO) throw ApiException.conflicto("Solo se registra el pago de un pedido confirmado");
        if (comprobanteRuta==null) throw ApiException.conflicto("El pedido no tiene comprobante de pago");
        estado=EstadoPedido.PAGADO;
        fechaPago=fechaActualizacion=Instant.now();
    }
    public BigDecimal getTotal() {
        if (fechaCotizacion==null) return null;
        return items.stream().map(ItemPedido::getSubtotal).reduce(cargoServicio,BigDecimal::add);
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
    public BigDecimal getCargoServicio() { return cargoServicio; }
    public String getNotasCotizacion() { return notasCotizacion; }
    public Instant getFechaCotizacion() { return fechaCotizacion; }
    public Instant getFechaConfirmacion() { return fechaConfirmacion; }
    public String getComprobanteRuta() { return comprobanteRuta; }
    public String getComprobanteNombre() { return comprobanteNombre; }
    public String getComprobanteTipo() { return comprobanteTipo; }
    public Long getComprobanteTamano() { return comprobanteTamano; }
    public Instant getFechaComprobante() { return fechaComprobante; }
    public Instant getFechaPago() { return fechaPago; }
    public List<ItemPedido> getItems() { return Collections.unmodifiableList(items); }
}
