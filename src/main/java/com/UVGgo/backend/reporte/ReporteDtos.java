package com.UVGgo.backend.reporte;

import com.UVGgo.backend.pedido.EstadoPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reporte del periodo. Todo se filtra por fecha de solicitud (hora de Guatemala) y los montos van en quetzales.
 * "Pedido" = solicitud con la cotización aceptada (confirmada en adelante, sin cancelar).
 */
public final class ReporteDtos {
    private ReporteDtos() {}

    public record Reporte(LocalDate desde, LocalDate hasta, Resumen resumen, Map<EstadoPedido,Long> porEstado,
                          List<Dia> porDia, Usuarios usuarios, List<PilotoFila> pilotos,
                          List<ProveedorFila> proveedores, List<ProductoFila> productos) {}

    /**
     * cargosEnvio: el cargo de servicio de cada pedido, que es lo que se cobra por llevarlo.
     * costoEnvioPorKm: cargos de envío de los pedidos con recorrido ÷ sus kilómetros; null sin recorridos.
     * horasCotizacion: de la solicitud a la cotización. horasPago: de la confirmación al pago registrado.
     * velocidadPromedio: km/h de los recorridos (kilómetros ÷ tiempo en movimiento); null sin recorridos medidos.
     */
    public record Resumen(long solicitudes, long pedidos, long cancelados,
                          BigDecimal montoProductos, BigDecimal cargosEnvio, BigDecimal totalVendido,
                          BigDecimal totalCobrado, BigDecimal porCobrar, BigDecimal ticketPromedio,
                          BigDecimal kilometros, BigDecimal costoEnvioPorKm,
                          Double horasCotizacion, Double horasPago, BigDecimal velocidadPromedio) {}

    public record Dia(LocalDate fecha, long solicitudes, long pedidos, BigDecimal vendido, BigDecimal kilometros) {}

    public record Usuarios(long estudiantes, long estudiantesNuevos, long estudiantesConSolicitud,
                           long pilotos, long pilotosActivos, long administradores, List<ClienteFila> topEstudiantes) {}

    public record ClienteFila(UUID id, String nombre, String carne, long pedidos, BigDecimal total) {}

    /** enCurso: pedidos del periodo que el piloto todavía tiene por entregar. Velocidades en km/h; null sin recorridos. */
    public record PilotoFila(UUID id, String nombre, boolean activo, long pedidos, long enCurso,
                             BigDecimal kilometros, BigDecimal kmPorPedido, BigDecimal cargosEnvio,
                             BigDecimal velocidadPromedio, BigDecimal velocidadMaxima) {}

    /** Tienda de donde salen los productos cotizados; los textos libres sin tienda van como "Sin tienda". */
    public record ProveedorFila(String tienda, long productos, BigDecimal unidades, long pedidos, BigDecimal monto) {}

    public record ProductoFila(String descripcion, String tienda, BigDecimal unidades, BigDecimal monto, long pedidos) {}
}
