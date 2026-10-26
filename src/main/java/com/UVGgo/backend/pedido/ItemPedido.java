package com.UVGgo.backend.pedido;

import com.UVGgo.backend.domain.Articulo;
import jakarta.persistence.*;
import java.math.*;
import java.util.UUID;

// Copia del producto al momento de solicitarlo: la cotización no cambia si el artículo se edita o elimina.
@Entity
@Table(name="items_pedido")
public class ItemPedido {
    public enum Tipo { CATALOGO, WEB, TEXTO }
    @Id private UUID id=UUID.randomUUID();
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="pedido_id") private Pedido pedido;
    @Enumerated(EnumType.STRING) @Column(length=20) private Tipo tipo;
    private Integer articuloId;
    @Column(nullable=false, length=500) private String descripcion;
    @Column(nullable=false, precision=12, scale=3) private BigDecimal cantidad;
    @Column(length=2000) private String url;
    @Column(length=2000) private String imagen;
    @Column(length=150) private String tienda;
    @Column(precision=12, scale=2) private BigDecimal precioReferencia;
    @Column(length=10) private String monedaReferencia;
    @Column(precision=12, scale=2) private BigDecimal precioUnitario;
    // Posible coincidencia con el catálogo para un ítem de texto: el administrador la ve al cotizar
    private Integer sugerenciaArticuloId;
    @Column(length=300) private String sugerenciaDescripcion;
    @Column(precision=12, scale=2) private BigDecimal sugerenciaPrecio;
    @Column(length=10) private String sugerenciaMoneda;
    protected ItemPedido() {}
    ItemPedido(Pedido pedido, Articulo a, int cantidad) {
        this(pedido,Tipo.CATALOGO,a.nombre(),cantidad,a.url(),a.imagen(),a.tienda(),a.precio(),a.moneda());
        this.articuloId=a.id();
    }
    ItemPedido(Pedido pedido, PedidoDtos.ProductoWeb w, int cantidad) {
        this(pedido,Tipo.WEB,w.titulo().strip(),cantidad,w.url().strip(),w.imagen(),w.tienda(),w.precio(),w.moneda());
    }
    // Material sin enlace ni artículo: solo descripción y cantidad; el administrador le pone precio al cotizar
    ItemPedido(Pedido pedido, String texto, int cantidad) {
        this(pedido,Tipo.TEXTO,texto.strip(),cantidad,null,null,null,null,null);
    }
    private ItemPedido(Pedido pedido, Tipo tipo, String descripcion, int cantidad, String url, String imagen,
                       String tienda, BigDecimal precioReferencia, String monedaReferencia) {
        this.pedido=pedido; this.tipo=tipo; this.descripcion=descripcion; this.cantidad=BigDecimal.valueOf(cantidad);
        this.url=url; this.imagen=imagen; this.tienda=tienda;
        this.precioReferencia=precioReferencia; this.monedaReferencia=monedaReferencia;
    }
    // El material de texto resultó ser un artículo del catálogo: pasa a ser un ítem del catálogo
    void enlazar(Articulo a) {
        if (tipo!=Tipo.TEXTO) return;
        tipo=Tipo.CATALOGO; articuloId=a.id(); descripcion=a.nombre(); url=a.url(); imagen=a.imagen(); tienda=a.tienda();
        precioReferencia=a.precio(); monedaReferencia=a.moneda();
        sugerenciaArticuloId=null; sugerenciaDescripcion=null; sugerenciaPrecio=null; sugerenciaMoneda=null;
    }
    void sugerir(Articulo a) {
        if (tipo!=Tipo.TEXTO) return;
        sugerenciaArticuloId=a.id(); sugerenciaDescripcion=a.nombre().length()>300 ? a.nombre().substring(0,300) : a.nombre();
        sugerenciaPrecio=a.precio(); sugerenciaMoneda=a.moneda();
    }
    void cotizar(BigDecimal precioUnitario) { this.precioUnitario=precioUnitario; }
    public BigDecimal getSubtotal() {
        return precioUnitario==null ? null : precioUnitario.multiply(cantidad).setScale(2,RoundingMode.HALF_UP);
    }
    public UUID getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public Integer getArticuloId() { return articuloId; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getCantidad() { return cantidad; }
    public String getUrl() { return url; }
    public String getImagen() { return imagen; }
    public String getTienda() { return tienda; }
    public BigDecimal getPrecioReferencia() { return precioReferencia; }
    public String getMonedaReferencia() { return monedaReferencia; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public Integer getSugerenciaArticuloId() { return sugerenciaArticuloId; }
    public String getSugerenciaDescripcion() { return sugerenciaDescripcion; }
    public BigDecimal getSugerenciaPrecio() { return sugerenciaPrecio; }
    public String getSugerenciaMoneda() { return sugerenciaMoneda; }
}
