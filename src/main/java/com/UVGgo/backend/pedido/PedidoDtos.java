package com.UVGgo.backend.pedido;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class PedidoDtos {
    private PedidoDtos() {}
    // Moneda en la que el administrador cotiza.
    public static final String MONEDA_COTIZACION="GTQ";
    // Producto encontrado en la web (Google Shopping); precio y moneda son solo de referencia.
    public record ProductoWeb(@NotBlank @Size(max=500) String titulo,
                              @NotBlank @Size(max=2000) @Pattern(regexp="https?://\\S+") String url,
                              @Size(max=2000) String imagen,
                              @Size(max=150) String tienda,
                              @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal precio,
                              @Size(max=10) String moneda) {}
    // Cada producto viene del catálogo (articuloId) o de la web (web), nunca de ambos.
    public record Producto(@Positive Integer articuloId, @Valid ProductoWeb web,
                           @NotNull @Min(1) @Max(999) Integer cantidad) {
        @JsonIgnore @AssertTrue(message="Indique articuloId o web, uno de los dos")
        public boolean isOrigenUnico() { return (articuloId==null)!=(web==null); }
    }
    public record Solicitud(@NotBlank @Size(max=150) String curso,
                            @NotBlank @Size(max=150) String nombreCatedratico,
                            @NotNull @FutureOrPresent LocalDate fechaLimite,
                            @Size(max=2000) String observaciones,
                            @Size(max=150) String puntoEntregaPreferido,
                            @NotEmpty @Size(max=100) List<@NotNull @Valid Producto> productos) {}
    public record Edicion(@NotNull @PositiveOrZero Long version, @NotNull @Valid Solicitud solicitud) {}
    public record Cancelacion(@NotNull @PositiveOrZero Long version) {}
    public record Aceptacion(@NotNull @PositiveOrZero Long version) {}
    public record Pago(@NotNull @PositiveOrZero Long version) {}
    public record Comprobante(String nombre, String tipo, long tamano, Instant fecha) {}
    public record Archivo(String nombre, String tipo, byte[] contenido) {}
    public record PrecioItem(@NotNull UUID itemId,
                             @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal precioUnitario) {}
    public record Cotizacion(@NotNull @PositiveOrZero Long version,
                             @NotEmpty @Size(max=100) List<@NotNull @Valid PrecioItem> precios,
                             @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal cargoServicio,
                             @Size(max=2000) String notas) {}
    public record Item(UUID id, ItemPedido.Tipo tipo, Integer articuloId, String descripcion, BigDecimal cantidad,
                       String url, String imagen, String tienda, BigDecimal precioReferencia, String monedaReferencia,
                       BigDecimal precioUnitario, BigDecimal subtotal) {
        static Item de(ItemPedido i) {
            return new Item(i.getId(),i.getTipo(),i.getArticuloId(),i.getDescripcion(),i.getCantidad(),i.getUrl(),
                    i.getImagen(),i.getTienda(),i.getPrecioReferencia(),i.getMonedaReferencia(),
                    i.getPrecioUnitario(),i.getSubtotal());
        }
    }
    // Los campos de cotización son null mientras la solicitud no se haya cotizado.
    public record Detalle(UUID id, long version, UUID estudianteId, String curso, String nombreCatedratico,
                          LocalDate fechaLimite, Instant fechaSolicitud, EstadoPedido estado,
                          String observaciones, String puntoEntregaPreferido, List<Item> productos,
                          String monedaCotizacion, BigDecimal cargoServicio, BigDecimal total, String notasCotizacion,
                          Instant fechaCotizacion, Instant fechaConfirmacion, Comprobante comprobante, Instant fechaPago) {
        public static Detalle de(Pedido p) {
            boolean cotizado=p.getFechaCotizacion()!=null;
            return new Detalle(p.getId(),p.getVersion(),p.getEstudiante().getId(),p.getCurso(),p.getNombreCatedratico(),
                    p.getFechaLimite(),p.getFechaSolicitud(),p.getEstado(),p.getObservaciones(),p.getPuntoEntregaPreferido(),
                    p.getItems().stream().map(Item::de).toList(),
                    cotizado ? MONEDA_COTIZACION : null,p.getCargoServicio(),p.getTotal(),p.getNotasCotizacion(),
                    p.getFechaCotizacion(),p.getFechaConfirmacion(),
                    p.getComprobanteRuta()==null ? null : new Comprobante(p.getComprobanteNombre(),p.getComprobanteTipo(),
                            p.getComprobanteTamano(),p.getFechaComprobante()),
                    p.getFechaPago());
        }
    }
    public record Pagina<T>(List<T> contenido, int pagina, int tamanio, long totalElementos, int totalPaginas) {}
}
