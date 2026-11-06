package com.UVGgo.backend.piloto;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="pilotos")
public class Piloto {
    @Id private UUID id=UUID.randomUUID();
    @Column(nullable=false, length=120) private String nombre;
    @Column(nullable=false) private boolean activo=true;
    protected Piloto() {}
    public Piloto(String nombre) { this.nombre=nombre; }
    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }
}
