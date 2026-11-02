package com.UVGgo.backend.ubicacion;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class UbicacionDtos {
    private UbicacionDtos() {}
    /** Lo que publica el repartidor (o el simulador). El rumbo va en grados de brújula: 0 = norte, 90 = este. */
    public record Posicion(@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
                           @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud,
                           @DecimalMin("0") @DecimalMax("360") Double rumbo) {}
    /** Lo que reciben los suscriptores: la posición más la hora en que el servidor la recibió. */
    public record Ubicacion(double latitud, double longitud, Double rumbo, Instant marca) {}
}
