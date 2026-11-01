package com.UVGgo.backend.listas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ListaMaterialesDtos {
    public record Material(
        @NotBlank @Size(max=120) String nombre,
        @NotBlank @Size(max=400) String descripcion,
        @NotNull @DecimalMin("0.001") @DecimalMax("999999999.999") @Digits(integer=9, fraction=3) BigDecimal cantidad,
        @NotBlank @Size(max=60) String unidad,
        @Size(max=1200) String observaciones,
        @Size(max=500) String referencia) {}
    public record Crear(@NotBlank @Size(max=120) String nombre,
                        @NotNull @Size(min=1,max=50) List<@NotNull @Valid Material> materiales,
                        @AssertTrue boolean revisada) {}
    public record Resumen(UUID id, String nombre, Instant creada) {}
    public record Detalle(UUID id, String nombre, Instant creada, List<Material> materiales, UUID solicitudId) {}
    public record Pagina(List<Resumen> listas, boolean hayMas) {}
}
