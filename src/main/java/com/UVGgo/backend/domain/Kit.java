package com.UVGgo.backend.domain;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class Kit {
    private int kitId;
    @NotBlank
    @Size(max = 120)
    private String name;
    @Size(max = 500)
    private String description;
    @NotBlank
    @Size(max = 150)
    private String course;
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;
    private boolean active = true;
    @NotNull
    @Valid
    private List<@NotNull Articulo> articulos = new ArrayList<>();
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer kitBaseId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private EstadoKit estado = EstadoKit.BASE;

    public List<Articulo> getArticulos() { return articulos; }
    public void setArticulos(List<Articulo> articulos) { this.articulos = articulos; }
    public Integer getKitBaseId() { return kitBaseId; }
    public void setKitBaseId(Integer kitBaseId) { this.kitBaseId = kitBaseId; }
    public EstadoKit getEstado() { return estado; }
    public void setEstado(EstadoKit estado) { this.estado = estado; }

    public int getKitId() {
        return kitId;
    }

    public void setKitId(int kitId) {
        this.kitId = kitId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
