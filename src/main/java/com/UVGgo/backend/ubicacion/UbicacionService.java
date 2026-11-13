package com.UVGgo.backend.ubicacion;

import com.UVGgo.backend.pedido.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Última posición conocida por pedido, solo en memoria: sirve a quien abre el mapa a mitad del recorrido.
 * Con cada posición calcula la velocidad del piloto y acumula en la base de datos lo que recorre con el pedido
 * (kilómetros, tiempo en movimiento y velocidad máxima).
 */
@Service
public class UbicacionService {
    // Con el piloto detenido el GPS "baila" unos metros: los movimientos menores no cuentan hasta sumar 10 m
    static final double MINIMO_KM=0.010;
    // Una moto en ciudad no pasa de esto; un salto más rápido es una mala lectura del GPS y no se cuenta
    static final double MAXIMO_KMH=150;
    // La velocidad se mide sobre los últimos 30 s: entre dos lecturas seguidas el ruido del GPS la haría saltar
    static final Duration VENTANA_VELOCIDAD=Duration.ofSeconds(30);
    // Con menos de 4 s de historia la medida no es confiable: se mantiene la anterior
    static final Duration MINIMO_VELOCIDAD=Duration.ofSeconds(4);
    // Un tramo tras más de 10 min sin señal suma su distancia, pero no su tiempo (el piloto pudo estar parado)
    static final Duration MAXIMO_TRAMO=Duration.ofMinutes(10);
    // Por debajo de esto el piloto cuenta como detenido (semáforo, tráfico o una parada)
    static final double DETENIDO_KMH=3;

    /** Lo recorrido desde la última posición contada; ceros si no se cuenta nada. */
    record Tramo(double km,long segundos) { static final Tramo NADA=new Tramo(0,0); }

    /** Estado del recorrido de un pedido. Solo se toca dentro de ConcurrentHashMap.compute (un hilo a la vez). */
    private static final class Recorrido {
        UbicacionDtos.Ubicacion ancla; // desde dónde se mide el siguiente tramo (la última posición que sí se contó)
        final Deque<UbicacionDtos.Ubicacion> ventana=new ArrayDeque<>();
        Double velocidad;
        Instant detenidoDesde;
        Tramo tramo=Tramo.NADA;
    }

    private final Map<UUID, UbicacionDtos.Ubicacion> ultimas=new ConcurrentHashMap<>();
    private final Map<UUID, Recorrido> recorridos=new ConcurrentHashMap<>();
    private final PedidoRepository pedidos;
    private final Clock reloj;

    @Autowired public UbicacionService(PedidoRepository pedidos) { this(pedidos,Clock.systemUTC()); }
    UbicacionService(PedidoRepository pedidos,Clock reloj) { this.pedidos=pedidos; this.reloj=reloj; }

    public UbicacionDtos.Ubicacion registrar(UUID pedidoId,UbicacionDtos.Posicion p) {
        Instant marca=Instant.now(reloj);
        Recorrido r=recorridos.compute(pedidoId,(id,actual) -> avanzar(actual==null ? new Recorrido() : actual,p,marca));
        Double velocidad; Instant detenidoDesde; Tramo tramo;
        synchronized (r) { velocidad=r.velocidad; detenidoDesde=r.detenidoDesde; tramo=r.tramo; }
        var u=new UbicacionDtos.Ubicacion(p.latitud(),p.longitud(),p.rumbo(),marca,velocidad,detenidoDesde);
        ultimas.put(pedidoId,u);
        if (tramo.km()>0) {
            pedidos.registrarTramo(pedidoId,BigDecimal.valueOf(tramo.km()).setScale(3,RoundingMode.HALF_UP),tramo.segundos(),
                    BigDecimal.valueOf(velocidad==null ? 0 : velocidad).setScale(2,RoundingMode.HALF_UP));
        }
        return u;
    }
    public Optional<UbicacionDtos.Ubicacion> ultima(UUID pedidoId) { return Optional.ofNullable(ultimas.get(pedidoId)); }

    private static Recorrido avanzar(Recorrido r,UbicacionDtos.Posicion p,Instant marca) {
        synchronized (r) {
            var actual=new UbicacionDtos.Ubicacion(p.latitud(),p.longitud(),p.rumbo(),marca,null,null);
            r.velocidad=velocidad(r,actual);
            // Sin medida (arranque o tras perder señal) no se sabe: se deja como estaba
            if (r.velocidad!=null) r.detenidoDesde=r.velocidad<DETENIDO_KMH ? (r.detenidoDesde!=null ? r.detenidoDesde : marca) : null;
            r.tramo=tramo(r,actual);
            return r;
        }
    }

    /** km/h entre la lectura más antigua de la ventana y la actual; null si se perdió la señal. */
    private static Double velocidad(Recorrido r,UbicacionDtos.Ubicacion actual) {
        r.ventana.addLast(actual);
        while (Duration.between(r.ventana.peekFirst().marca(),actual.marca()).compareTo(VENTANA_VELOCIDAD)>0) r.ventana.removeFirst();
        if (r.ventana.size()==1) return null; // primera lectura o más de 30 s sin señal
        var base=r.ventana.peekFirst();
        Duration dt=Duration.between(base.marca(),actual.marca());
        if (dt.compareTo(MINIMO_VELOCIDAD)<0) return r.velocidad;
        double km=Distancia.km(base.latitud(),base.longitud(),actual.latitud(),actual.longitud());
        double kmh=km/(dt.toMillis()/3_600_000.0);
        if (kmh>MAXIMO_KMH) return r.velocidad; // salto del GPS: se queda la medida anterior
        return Math.round(kmh*10)/10.0;
    }

    /** Kilómetros y segundos desde la última posición contada; nada si es la primera, es ruido o es un salto imposible. */
    private static Tramo tramo(Recorrido r,UbicacionDtos.Ubicacion actual) {
        var ancla=r.ancla;
        if (ancla==null) { r.ancla=actual; return Tramo.NADA; } // primera posición (o el servidor se reinició)
        double km=Distancia.km(ancla.latitud(),ancla.longitud(),actual.latitud(),actual.longitud());
        if (km<MINIMO_KM) return Tramo.NADA;
        r.ancla=actual; // un salto descartado también mueve el ancla: el recorrido sigue desde la lectura nueva
        Duration dt=Duration.between(ancla.marca(),actual.marca());
        double horas=dt.toMillis()/3_600_000.0;
        if (horas<=0 || km/horas>MAXIMO_KMH) return Tramo.NADA;
        return new Tramo(km,dt.compareTo(MAXIMO_TRAMO)>0 ? 0 : dt.toSeconds());
    }
}
