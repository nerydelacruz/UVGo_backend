package com.UVGgo.backend.piloto;

import com.UVGgo.backend.usuario.Usuario;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Tercer tipo de usuario: el conductor que entrega los pedidos. Inicia sesión como cualquier usuario
 * (correo y contraseña) y desde la app móvil ve sus pedidos asignados y publica su ubicación.
 * Las cuentas las crea un administrador (POST /api/v1/pilotos); no hay registro público.
 */
@Entity
@DiscriminatorValue("PILOTO")
public class Piloto extends Usuario {
    protected Piloto() {}
    public Piloto(String nombre,String correo,String telefono,String hash) { super(nombre,correo,telefono,hash); }
    @Override public String obtenerRol() { return "PILOTO"; }
}
