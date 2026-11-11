package com.UVGgo.backend.piloto;

import com.UVGgo.backend.pedido.EstadoPedido;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class PilotoDtos {
    private PilotoDtos() {}
    public record Resumen(UUID id, String nombre) {
        static Resumen de(Piloto p) { return new Resumen(p.getId(),p.getNombre()); }
    }
    /** Alta de un piloto por un administrador; mismas reglas de teléfono y contraseña que el registro de estudiantes. */
    public record Alta(@NotBlank @Size(max=120) String nombre,
                       @NotBlank @Email @Size(max=254) String correo,
                       @NotBlank @Pattern(regexp="[+0-9 ()-]{8,25}") String telefono,
                       @NotBlank @Size(min=12, max=72) String contrasena) {}
    public record Asignar(@NotNull @PositiveOrZero Long version, @NotNull UUID pilotoId) {}
    /**
     * Lo que recibe el piloto por WebSocket. asignado=false avisa que el pedido ya no es suyo.
     * destino es el punto de entrega que pidió el estudiante; puede ser null.
     */
    public record Asignacion(UUID pedidoId, String curso, String destino, EstadoPedido estado, boolean asignado, Instant marca) {}
}
