package com.UVGgo.backend.usuario;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("ESTUDIANTE")
public class Estudiante extends Usuario {
    @Column(unique = true, length = 30) private String carne;
    @Column(length = 120) private String carrera;
    protected Estudiante() {}
    public Estudiante(String nombre, String correo, String telefono, String hash, String carne, String carrera) {
        super(nombre, correo, telefono, hash); this.carne = carne; this.carrera = carrera;
    }
    @Override public String obtenerRol() { return "ESTUDIANTE"; }
    public String getCarne() { return carne; }
    public String getCarrera() { return carrera; }
}
