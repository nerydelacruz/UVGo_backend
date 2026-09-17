package com.UVGgo.backend.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class PedidoDtos {
    private PedidoDtos() {}
    public record Material(@NotBlank @Size(max=500) String descripcion,
                           @NotNull @DecimalMin("0.001") @Digits(integer=9,fraction=3) BigDecimal cantidad,
                           @NotBlank @Size(max=40) String unidadMedida) {}
    public record Solicitud(@NotBlank @Size(max=150) String curso,
                            @NotBlank @Size(max=150) String nombreCatedratico,
                            @NotNull @FutureOrPresent LocalDate fechaLimite,
                            @Size(max=2000) String observaciones,
                            @Size(max=150) String puntoEntregaPreferido,
                            @NotEmpty @Size(max=100) List<@NotNull @Valid Material> materiales) {}
    public record Edicion(@NotNull @PositiveOrZero Long version, @NotNull @Valid Solicitud solicitud) {}
    public record Cancelacion(@NotNull @PositiveOrZero Long version) {}
    public record Item(UUID id, String descripcion, BigDecimal cantidad, String unidadMedida) {}
    public record Detalle(UUID id, long version, UUID estudianteId, String curso, String nombreCatedratico,
                          LocalDate fechaLimite, Instant fechaSolicitud, EstadoPedido estado,
                          String observaciones, String puntoEntregaPreferido, List<Item> materiales) {
        public static Detalle de(Pedido p) {
            return new Detalle(p.getId(),p.getVersion(),p.getEstudiante().getId(),p.getCurso(),p.getNombreCatedratico(),
                    p.getFechaLimite(),p.getFechaSolicitud(),p.getEstado(),p.getObservaciones(),p.getPuntoEntregaPreferido(),
                    p.getItems().stream().map(i -> new Item(i.getId(),i.getDescripcion(),i.getCantidad(),i.getUnidadMedida())).toList());
        }
    }
    public record Pagina<T>(List<T> contenido, int pagina, int tamanio, long totalElementos, int totalPaginas) {}
}
