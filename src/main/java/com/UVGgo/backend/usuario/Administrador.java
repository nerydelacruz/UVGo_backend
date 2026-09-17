package com.UVGgo.backend.usuario;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("ADMINISTRADOR")
public class Administrador extends Usuario {
    @Column(length = 40) private String codigoAdministrador;
    protected Administrador() {}
    public Administrador(String nombre, String correo, String telefono, String hash, String codigo) {
        super(nombre, correo, telefono, hash); this.codigoAdministrador = codigo;
    }
    @Override public String obtenerRol() { return "ADMINISTRADOR"; }
}
