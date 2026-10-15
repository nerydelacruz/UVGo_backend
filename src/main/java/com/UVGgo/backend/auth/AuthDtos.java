package com.UVGgo.backend.auth;

import com.UVGgo.backend.usuario.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}
    public record Registro(
            @NotBlank @Size(max=120) String nombre,
            @NotBlank @Email @Size(max=254) String correo,
            @NotBlank @Pattern(regexp="[+0-9 ()-]{8,25}") String telefono,
            @NotBlank @Pattern(regexp="[0-9]{4,30}") String carne,
            @NotBlank @Size(min=12, max=72) String contrasena,
            @Size(max=120) String carrera) {}
    public record Login(@NotBlank @Email @Size(max=254) String correo,
                        @NotBlank @Size(max=72) String contrasena) {}
    public record Contacto(@NotBlank @Pattern(regexp="[+0-9 ()-]{8,25}") String telefono,
                           @Email @Size(max=254) String correoContacto) {}
    public record Perfil(UUID id, String nombre, String correo, String telefono, String correoContacto,
                         String rol, String carne, String carrera, boolean correoVerificado) {
        public static Perfil de(Usuario u) {
            return new Perfil(u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(), u.getCorreoContacto(),
                    u.obtenerRol(), u instanceof Estudiante e ? e.getCarne() : null,
                    u instanceof Estudiante e ? e.getCarrera() : null, u.isCorreoVerificado());
        }
    }
    public record Token(String accessToken, String tokenType, Instant expiresAt, Perfil usuario) {}
}
