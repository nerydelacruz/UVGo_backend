package com.UVGgo.backend.listas;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listas_materiales")
public class ListaMateriales {
    @Id private UUID id;
    @Column(nullable = false) private UUID usuarioId;
    @Column(nullable = false, length = 120) private String nombre;
    @Column(nullable = false) private Instant creada;
    @Column(nullable = false, columnDefinition = "text") private String materiales;
    @Column(unique = true) private UUID solicitudId;
    protected ListaMateriales() {}
    ListaMateriales(UUID usuarioId, String nombre, String materiales) {
        this.id = UUID.randomUUID(); this.usuarioId = usuarioId;
        this.nombre = nombre.strip(); this.materiales = materiales; this.creada = Instant.now();
    }
    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public Instant getCreada() { return creada; }
    public String getMateriales() { return materiales; }
    public UUID getSolicitudId() { return solicitudId; }
    void vincularSolicitud(UUID solicitudId) { this.solicitudId=solicitudId; }
}
