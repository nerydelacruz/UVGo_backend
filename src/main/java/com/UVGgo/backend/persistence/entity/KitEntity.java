package com.UVGgo.backend.persistence.entity;

import jakarta.persistence.*;
import com.UVGgo.backend.domain.EstadoKit;
import java.util.ArrayList;
import java.util.List;

import java.math.BigDecimal;

@Entity
@Table(name = "kits")
public class KitEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_kit")
    private Integer idKit;

    private String nombre;
    private String descripcion;
    private String curso;
    private BigDecimal precio;
    private Boolean estado;
    private Integer kitBaseId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoKit estadoPersonalizacion = EstadoKit.BASE;
    @OneToMany(mappedBy = "kit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ArticuloEntity> articulos = new ArrayList<>();
    private Integer usuarioId;

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }
    public Integer getKitBaseId() { return kitBaseId; }
    public void setKitBaseId(Integer kitBaseId) { this.kitBaseId = kitBaseId; }
    public EstadoKit getEstadoPersonalizacion() { return estadoPersonalizacion; }
    public void setEstadoPersonalizacion(EstadoKit estadoPersonalizacion) { this.estadoPersonalizacion = estadoPersonalizacion; }
    public List<ArticuloEntity> getArticulos() { return articulos; }
    public void setArticulos(List<ArticuloEntity> articulos) { this.articulos = articulos; }

    public Integer getIdKit() {
        return idKit;
    }

    public void setIdKit(Integer idKit) {
        this.idKit = idKit;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCurso() {
        return curso;
    }

    public void setCurso(String curso) {
        this.curso = curso;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }
}
