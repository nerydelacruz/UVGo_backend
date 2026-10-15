package com.UVGgo.backend.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "articulos")
public class ArticuloEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, length = 300)
    private String nombre;
    @Column(nullable = false, length = 4000)
    private String descripcion;
    @Column(length = 100)
    private String categoria;
    @Column(length = 2000)
    private String imagen;
    @Column(precision = 12, scale = 2)
    private BigDecimal precio;
    @Column(length = 10)
    private String moneda;
    @Column(length = 150)
    private String tienda;
    @Column(length = 2000)
    private String url;
    @Column(name = "id_externo", unique = true)
    private String idExterno;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getTienda() { return tienda; }
    public void setTienda(String tienda) { this.tienda = tienda; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getIdExterno() { return idExterno; }
    public void setIdExterno(String idExterno) { this.idExterno = idExterno; }
}
