package com.UVGgo.backend.reporte;

import com.UVGgo.backend.api.ApiException;
import com.UVGgo.backend.pedido.EstadoPedido;
import com.UVGgo.backend.pedido.ItemPedido;
import com.UVGgo.backend.pedido.Pedido;
import com.UVGgo.backend.pedido.PedidoRepository;
import com.UVGgo.backend.piloto.Piloto;
import com.UVGgo.backend.piloto.PilotoRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Arma el reporte del periodo en memoria a partir de las solicitudes del rango (una consulta) y unos conteos.
 * Para el volumen de UVGo es más simple y portable (PostgreSQL y H2) que agregar en SQL.
 */
@Service
@Transactional(readOnly=true)
public class ReporteService {
    static final ZoneId ZONA=ZoneId.of("America/Guatemala");
    static final int MAXIMO_DIAS=366;
    private static final int TOP=10;
    // Cotización aceptada y no cancelada: cuenta como venta
    private static final Set<EstadoPedido> VENDIDOS=EnumSet.of(EstadoPedido.CONFIRMADO,EstadoPedido.EN_COMPRA,
            EstadoPedido.LISTO,EstadoPedido.ENTREGADO,EstadoPedido.PAGADO);
    private static final String SIN_TIENDA="Sin tienda";

    private final PedidoRepository pedidos;
    private final PilotoRepository pilotos;
    private final EntityManager em;
    private final Clock reloj;

    public ReporteService(PedidoRepository pedidos,PilotoRepository pilotos,EntityManager em) {
        this.pedidos=pedidos; this.pilotos=pilotos; this.em=em; this.reloj=Clock.system(ZONA);
    }

    /** Sin fechas: los últimos 30 días hasta hoy. */
    public ReporteDtos.Reporte generar(LocalDate desde,LocalDate hasta) {
        LocalDate fin=hasta!=null ? hasta : LocalDate.now(reloj);
        LocalDate inicio=desde!=null ? desde : fin.minusDays(29);
        if (inicio.isAfter(fin)) throw new ApiException(HttpStatus.BAD_REQUEST,"La fecha inicial no puede ser posterior a la final");
        if (ChronoUnit.DAYS.between(inicio,fin)+1>MAXIMO_DIAS)
            throw new ApiException(HttpStatus.BAD_REQUEST,"El periodo no puede superar "+MAXIMO_DIAS+" días");

        Instant desdeInstante=inicio.atStartOfDay(ZONA).toInstant(), hastaInstante=fin.plusDays(1).atStartOfDay(ZONA).toInstant();
        List<Pedido> solicitudes=pedidos.findParaReporte(desdeInstante,hastaInstante);
        List<Pedido> vendidos=solicitudes.stream().filter(p -> VENDIDOS.contains(p.getEstado())).toList();

        return new ReporteDtos.Reporte(inicio,fin,resumen(solicitudes,vendidos),porEstado(solicitudes),
                porDia(inicio,fin,solicitudes),usuarios(solicitudes,vendidos,desdeInstante,hastaInstante),
                pilotos(solicitudes),proveedores(vendidos),productos(vendidos));
    }

    private ReporteDtos.Resumen resumen(List<Pedido> solicitudes,List<Pedido> vendidos) {
        BigDecimal productos=suma(vendidos,p -> sumaItems(p.getItems()));
        BigDecimal envio=suma(vendidos,Pedido::getCargoServicio);
        BigDecimal vendido=productos.add(envio);
        BigDecimal cobrado=suma(vendidos.stream().filter(p -> p.getFechaPago()!=null).toList(),Pedido::getTotal);
        List<Pedido> conRecorrido=vendidos.stream().filter(p -> p.getKilometros().signum()>0).toList();
        BigDecimal kmConRecorrido=suma(conRecorrido,Pedido::getKilometros);
        long cancelados=solicitudes.stream().filter(p -> p.getEstado()==EstadoPedido.CANCELADO).count();
        return new ReporteDtos.Resumen(solicitudes.size(),vendidos.size(),cancelados,
                productos,envio,vendido,cobrado,vendido.subtract(cobrado),dividir(vendido,vendidos.size()),
                km(suma(solicitudes,Pedido::getKilometros)),
                kmConRecorrido.signum()>0 ? dinero(suma(conRecorrido,Pedido::getCargoServicio).divide(kmConRecorrido,2,RoundingMode.HALF_UP)) : null,
                horasPromedio(solicitudes,Pedido::getFechaSolicitud,Pedido::getFechaCotizacion),
                horasPromedio(vendidos,Pedido::getFechaConfirmacion,Pedido::getFechaPago),
                velocidadPromedio(solicitudes));
    }

    private Map<EstadoPedido,Long> porEstado(List<Pedido> solicitudes) {
        Map<EstadoPedido,Long> conteo=new EnumMap<>(EstadoPedido.class);
        for (EstadoPedido e:EstadoPedido.values()) conteo.put(e,0L);
        solicitudes.forEach(p -> conteo.merge(p.getEstado(),1L,Long::sum));
        return conteo;
    }

    /** Un punto por día del periodo, también los días sin movimiento (la gráfica no salta días). */
    private List<ReporteDtos.Dia> porDia(LocalDate inicio,LocalDate fin,List<Pedido> solicitudes) {
        Map<LocalDate,List<Pedido>> porFecha=solicitudes.stream()
                .collect(Collectors.groupingBy(p -> LocalDate.ofInstant(p.getFechaSolicitud(),ZONA)));
        List<ReporteDtos.Dia> dias=new ArrayList<>();
        for (LocalDate d=inicio; !d.isAfter(fin); d=d.plusDays(1)) {
            List<Pedido> del=porFecha.getOrDefault(d,List.of());
            List<Pedido> vendidos=del.stream().filter(p -> VENDIDOS.contains(p.getEstado())).toList();
            dias.add(new ReporteDtos.Dia(d,del.size(),vendidos.size(),suma(vendidos,Pedido::getTotal),km(suma(del,Pedido::getKilometros))));
        }
        return dias;
    }

    private ReporteDtos.Usuarios usuarios(List<Pedido> solicitudes,List<Pedido> vendidos,Instant desde,Instant hasta) {
        long estudiantes=contar("select count(e) from Estudiante e",null,null);
        long nuevos=contar("select count(e) from Estudiante e where e.fechaRegistro >= :desde and e.fechaRegistro < :hasta",desde,hasta);
        long pilotosActivos=contar("select count(p) from Piloto p where p.activo = true",null,null);
        long administradores=contar("select count(a) from Administrador a",null,null);
        long conSolicitud=solicitudes.stream().map(p -> p.getEstudiante().getId()).distinct().count();

        Map<UUID,List<Pedido>> porEstudiante=vendidos.stream().collect(Collectors.groupingBy(p -> p.getEstudiante().getId()));
        List<ReporteDtos.ClienteFila> top=porEstudiante.values().stream()
                .map(lista -> {
                    var e=lista.get(0).getEstudiante();
                    return new ReporteDtos.ClienteFila(e.getId(),e.getNombre(),e.getCarne(),lista.size(),suma(lista,Pedido::getTotal));
                })
                .sorted(Comparator.comparing(ReporteDtos.ClienteFila::total).reversed().thenComparing(ReporteDtos.ClienteFila::nombre))
                .limit(TOP).toList();
        return new ReporteDtos.Usuarios(estudiantes,nuevos,conSolicitud,pilotos.count(),pilotosActivos,administradores,top);
    }

    /** Todos los pilotos, también los que no tuvieron pedidos en el periodo (se ve quién está libre). */
    private List<ReporteDtos.PilotoFila> pilotos(List<Pedido> solicitudes) {
        Map<UUID,List<Pedido>> porPiloto=solicitudes.stream().filter(p -> p.getPiloto()!=null)
                .collect(Collectors.groupingBy(p -> p.getPiloto().getId()));
        return pilotos.findAll().stream()
                .map(piloto -> fila(piloto,porPiloto.getOrDefault(piloto.getId(),List.of())))
                .sorted(Comparator.comparingLong(ReporteDtos.PilotoFila::pedidos).reversed()
                        .thenComparing(ReporteDtos.PilotoFila::kilometros,Comparator.reverseOrder())
                        .thenComparing(ReporteDtos.PilotoFila::nombre))
                .toList();
    }
    private ReporteDtos.PilotoFila fila(Piloto piloto,List<Pedido> asignados) {
        long enCurso=asignados.stream().filter(p -> Pedido.ESTADOS_ENTREGABLES.contains(p.getEstado())).count();
        BigDecimal km=suma(asignados,Pedido::getKilometros);
        long conRecorrido=asignados.stream().filter(p -> p.getKilometros().signum()>0).count();
        BigDecimal envio=suma(asignados.stream().filter(p -> VENDIDOS.contains(p.getEstado())).toList(),Pedido::getCargoServicio);
        BigDecimal maxima=asignados.stream().map(Pedido::getVelocidadMaxima).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).map(v -> v.setScale(1,RoundingMode.HALF_UP)).orElse(null);
        return new ReporteDtos.PilotoFila(piloto.getId(),piloto.getNombre(),piloto.isActivo(),asignados.size(),enCurso,
                km(km),conRecorrido>0 ? km.divide(BigDecimal.valueOf(conRecorrido),2,RoundingMode.HALF_UP) : null,envio,
                velocidadPromedio(asignados),maxima);
    }

    private List<ReporteDtos.ProveedorFila> proveedores(List<Pedido> vendidos) {
        record Linea(String tienda,UUID pedido,ItemPedido item) {}
        Map<String,List<Linea>> porTienda=vendidos.stream()
                .flatMap(p -> p.getItems().stream().map(i -> new Linea(tienda(i),p.getId(),i)))
                .collect(Collectors.groupingBy(Linea::tienda));
        return porTienda.entrySet().stream()
                .map(e -> new ReporteDtos.ProveedorFila(e.getKey(),e.getValue().size(),
                        e.getValue().stream().map(l -> l.item().getCantidad()).reduce(BigDecimal.ZERO,BigDecimal::add),
                        e.getValue().stream().map(Linea::pedido).distinct().count(),
                        dinero(sumaItems(e.getValue().stream().map(Linea::item).toList()))))
                .sorted(Comparator.comparing(ReporteDtos.ProveedorFila::monto).reversed().thenComparing(ReporteDtos.ProveedorFila::tienda))
                .toList();
    }

    /** Los más pedidos por unidades. Se agrupan por artículo del catálogo o, si no hay, por el texto. */
    private List<ReporteDtos.ProductoFila> productos(List<Pedido> vendidos) {
        record Linea(String clave,UUID pedido,ItemPedido item) {}
        Map<String,List<Linea>> porProducto=vendidos.stream()
                .flatMap(p -> p.getItems().stream().map(i -> new Linea(
                        i.getArticuloId()!=null ? "#"+i.getArticuloId() : i.getDescripcion().strip().toLowerCase(Locale.ROOT),p.getId(),i)))
                .collect(Collectors.groupingBy(Linea::clave));
        return porProducto.values().stream()
                .map(lineas -> {
                    ItemPedido primero=lineas.get(0).item();
                    List<ItemPedido> items=lineas.stream().map(Linea::item).toList();
                    return new ReporteDtos.ProductoFila(primero.getDescripcion(),tienda(primero),
                            items.stream().map(ItemPedido::getCantidad).reduce(BigDecimal.ZERO,BigDecimal::add),
                            dinero(sumaItems(items)),lineas.stream().map(Linea::pedido).distinct().count());
                })
                .sorted(Comparator.comparing(ReporteDtos.ProductoFila::unidades).reversed()
                        .thenComparing(ReporteDtos.ProductoFila::monto,Comparator.reverseOrder()))
                .limit(TOP).toList();
    }

    // --- utilidades ---

    private long contar(String jpql,Instant desde,Instant hasta) {
        var q=em.createQuery(jpql,Long.class);
        if (desde!=null) q.setParameter("desde",desde).setParameter("hasta",hasta);
        return q.getSingleResult();
    }
    private static String tienda(ItemPedido i) {
        return i.getTienda()==null || i.getTienda().isBlank() ? SIN_TIENDA : i.getTienda().strip();
    }
    private static BigDecimal sumaItems(List<ItemPedido> items) {
        return items.stream().map(ItemPedido::getSubtotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    private static BigDecimal suma(List<Pedido> lista,Function<Pedido,BigDecimal> valor) {
        return dinero(lista.stream().map(valor).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add));
    }
    private static BigDecimal dividir(BigDecimal total,long entre) {
        return entre==0 ? BigDecimal.ZERO.setScale(2) : total.divide(BigDecimal.valueOf(entre),2,RoundingMode.HALF_UP);
    }
    private static BigDecimal dinero(BigDecimal v) { return v.setScale(2,RoundingMode.HALF_UP); }
    private static BigDecimal km(BigDecimal v) { return v.setScale(2,RoundingMode.HALF_UP); }
    /** km/h (1 decimal) de los pedidos con tiempo en movimiento medido; null si ninguno lo tiene. */
    private static BigDecimal velocidadPromedio(List<Pedido> lista) {
        List<Pedido> medidos=lista.stream().filter(p -> p.getSegundosEnRuta()>0).toList();
        long segundos=medidos.stream().mapToLong(Pedido::getSegundosEnRuta).sum();
        if (segundos==0) return null;
        BigDecimal horas=BigDecimal.valueOf(segundos).divide(BigDecimal.valueOf(3600),6,RoundingMode.HALF_UP);
        return suma(medidos,Pedido::getKilometros).divide(horas,1,RoundingMode.HALF_UP);
    }
    /** Promedio en horas (1 decimal) entre dos marcas; null si ningún pedido tiene ambas. */
    private static Double horasPromedio(List<Pedido> lista,Function<Pedido,Instant> desde,Function<Pedido,Instant> hasta) {
        OptionalDouble promedio=lista.stream()
                .filter(p -> desde.apply(p)!=null && hasta.apply(p)!=null)
                .mapToLong(p -> Duration.between(desde.apply(p),hasta.apply(p)).toMinutes())
                .average();
        return promedio.isPresent() ? Math.round(promedio.getAsDouble()/60*10)/10.0 : null;
    }
}
