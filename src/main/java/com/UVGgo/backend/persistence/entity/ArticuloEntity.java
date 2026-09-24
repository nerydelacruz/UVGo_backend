package com.UVGgo.backend.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "articulos")
public class ArticuloEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kit_id", nullable = false)
    private KitEntity kit;
    @Column(nullable = false, length = 120)
    private String nombre;
    @Column(nullable = false, length = 500)
    private String descripcion;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;
    @Column(length = 100)
    private String categoria;
    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion;
    @Column(length = 2000)
    private String observacionesCotizacion;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public KitEntity getKit() { return kit; }
    public void setKit(KitEntity kit) { this.kit = kit; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getObservacionesCotizacion() { return observacionesCotizacion; }
    public void setObservacionesCotizacion(String observacionesCotizacion) { this.observacionesCotizacion = observacionesCotizacion; }
}
