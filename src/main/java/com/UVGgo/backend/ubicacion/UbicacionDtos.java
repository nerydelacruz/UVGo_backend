package com.UVGgo.backend.ubicacion;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class UbicacionDtos {
    private UbicacionDtos() {}
    /** Lo que publica el piloto desde la app móvil. El rumbo va en grados de brújula: 0 = norte, 90 = este. */
    public record Posicion(@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
                           @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud,
                           @DecimalMin("0") @DecimalMax("360") Double rumbo) {}
    /**
     * Lo que reciben los suscriptores: la posición más la hora en que el servidor la recibió.
     * velocidad: km/h promedio de los últimos segundos (la calcula el servidor); null al arrancar o tras perder señal.
     * detenidoDesde: desde cuándo el piloto no se mueve (menos de 3 km/h); null si va en movimiento.
     */
    public record Ubicacion(double latitud, double longitud, Double rumbo, Instant marca, Double velocidad, Instant detenidoDesde) {}
}
