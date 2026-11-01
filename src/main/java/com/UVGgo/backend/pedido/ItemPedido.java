package com.UVGgo.backend.pedido;

import com.UVGgo.backend.domain.Articulo;
import jakarta.persistence.*;
import java.math.*;
import java.util.UUID;

// Copia del producto al momento de solicitarlo: la cotización no cambia si el artículo se edita o elimina.
@Entity
@Table(name="items_pedido")
public class ItemPedido {
    public enum Tipo { CATALOGO, WEB, TEXTO, LISTA }
    @Id private UUID id=UUID.randomUUID();
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="pedido_id") private Pedido pedido;
    @Enumerated(EnumType.STRING) @Column(length=20) private Tipo tipo;
    private Integer articuloId;
    @Column(nullable=false, length=500) private String descripcion;
    @Column(length=120) private String nombreMaterial;
    @Column(name="unidad_medida", length=60) private String unidad;
    @Column(length=1200) private String observacionesMaterial;
    @Column(length=500) private String referenciaMaterial;
    @Column(nullable=false, precision=12, scale=3) private BigDecimal cantidad;
    @Column(length=2000) private String url;
    @Column(length=2000) private String imagen;
    @Column(length=150) private String tienda;
    @Column(precision=12, scale=2) private BigDecimal precioReferencia;
    @Column(length=10) private String monedaReferencia;
    @Column(precision=12, scale=2) private BigDecimal precioUnitario;
    protected ItemPedido() {}
    ItemPedido(Pedido pedido, PedidoDtos.MaterialLibre material) {
        this.pedido=pedido; this.tipo=Tipo.LISTA;
        this.nombreMaterial=material.nombre(); this.descripcion=material.descripcion();
        this.cantidad=material.cantidad(); this.unidad=material.unidad();
        this.observacionesMaterial=material.observaciones(); this.referenciaMaterial=material.referencia();
    }
    ItemPedido(Pedido pedido, Articulo a, int cantidad) {
        this(pedido,Tipo.CATALOGO,a.nombre(),cantidad,a.url(),a.imagen(),a.tienda(),a.precio(),a.moneda());
        this.articuloId=a.id();
    }
    ItemPedido(Pedido pedido, PedidoDtos.ProductoWeb w, int cantidad) {
        this(pedido,Tipo.WEB,w.titulo().strip(),cantidad,w.url().strip(),w.imagen(),w.tienda(),w.precio(),w.moneda());
    }
    private ItemPedido(Pedido pedido, Tipo tipo, String descripcion, int cantidad, String url, String imagen,
                       String tienda, BigDecimal precioReferencia, String monedaReferencia) {
        this.pedido=pedido; this.tipo=tipo; this.descripcion=descripcion; this.cantidad=BigDecimal.valueOf(cantidad);
        this.url=url; this.imagen=imagen; this.tienda=tienda;
        this.precioReferencia=precioReferencia; this.monedaReferencia=monedaReferencia;
    }
    void cotizar(BigDecimal precioUnitario) { this.precioUnitario=precioUnitario; }
    public BigDecimal getSubtotal() {
        return precioUnitario==null ? null : precioUnitario.multiply(cantidad).setScale(2,RoundingMode.HALF_UP);
    }
    public UUID getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public Integer getArticuloId() { return articuloId; }
    public String getDescripcion() { return descripcion; }
    public String getNombreMaterial() { return nombreMaterial; }
    public String getUnidad() { return unidad; }
    public String getObservacionesMaterial() { return observacionesMaterial; }
    public String getReferenciaMaterial() { return referenciaMaterial; }
    public BigDecimal getCantidad() { return cantidad; }
    public String getUrl() { return url; }
    public String getImagen() { return imagen; }
    public String getTienda() { return tienda; }
    public BigDecimal getPrecioReferencia() { return precioReferencia; }
    public String getMonedaReferencia() { return monedaReferencia; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
}
