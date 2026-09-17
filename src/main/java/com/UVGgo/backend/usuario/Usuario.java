package com.UVGgo.backend.usuario;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
public abstract class Usuario {
    @Id private UUID id = UUID.randomUUID();
    @Column(nullable = false, length = 120) private String nombre;
    @Column(nullable = false, unique = true, length = 254) private String correo;
    @Column(nullable = false, length = 25) private String telefono;
    @Column(length = 254) private String correoContacto;
    @Column(nullable = false, length = 100) private String contrasenaHash;
    @Column(nullable = false) private boolean activo = true;
    @Column(nullable = false) private boolean correoVerificado;
    @Column(nullable = false) private Instant fechaRegistro = Instant.now();

    protected Usuario() {}
    protected Usuario(String nombre, String correo, String telefono, String hash) {
        this.nombre = nombre; this.correo = correo; this.telefono = telefono; this.contrasenaHash = hash;
    }
    public abstract String obtenerRol();
    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public String getCorreoContacto() { return correoContacto; }
    public String getContrasenaHash() { return contrasenaHash; }
    public boolean isActivo() { return activo; }
    public boolean isCorreoVerificado() { return correoVerificado; }
    public void actualizarContacto(String telefono, String correoContacto) {
        this.telefono = telefono; this.correoContacto = correoContacto;
    }
}
