package com.UVGgo.backend.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sesiones")
public class Sesion {
    @Id private UUID id;
    @Column(nullable = false) private UUID usuarioId;
    @Column(nullable = false) private Instant venceEn;
    @Column(nullable = false) private boolean revocada;
    protected Sesion() {}
    public Sesion(UUID id, UUID usuarioId, Instant venceEn) {
        this.id = id; this.usuarioId = usuarioId; this.venceEn = venceEn;
    }
    public boolean vigente(UUID usuario, Instant ahora) {
        return usuarioId.equals(usuario) && !revocada && venceEn.isAfter(ahora);
    }
    public void revocar() { revocada = true; }
}
