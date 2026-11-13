package com.UVGgo.backend.ubicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.UVGgo.backend.pedido.PedidoRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KilometrajeTests {
    /** Reloj que la prueba adelanta a mano. */
    static class RelojManual extends Clock {
        Instant ahora=Instant.parse("2026-11-12T15:00:00Z");
        void avanzar(Duration d) { ahora=ahora.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private final PedidoRepository pedidos=mock(PedidoRepository.class);
    private final RelojManual reloj=new RelojManual();
    private final UbicacionService servicio=new UbicacionService(pedidos,reloj);
    private final UUID pedido=UUID.randomUUID();

    private UbicacionDtos.Ubicacion publicar(double lat,double lon) { return servicio.registrar(pedido,new UbicacionDtos.Posicion(lat,lon,null)); }

    @Test void laDistanciaEntreDosPuntosConocidosEsCorrecta() {
        // 0.01° de latitud son ~1.112 km en cualquier parte del mundo
        assertThat(Distancia.km(14.60,-90.49,14.61,-90.49)).isCloseTo(1.112,within(0.002));
        assertThat(Distancia.km(14.60,-90.49,14.60,-90.49)).isZero();
    }
    @Test void laPrimeraPosicionSoloMarcaElInicio() {
        publicar(14.60,-90.49);
        verifyNoInteractions(pedidos);
    }
    @Test void unRecorridoNormalSumaSusKilometros() {
        publicar(14.60,-90.49);
        reloj.avanzar(Duration.ofMinutes(3)); // ~1.1 km en 3 min ≈ 22 km/h
        publicar(14.61,-90.49);
        var km=ArgumentCaptor.forClass(BigDecimal.class);
        verify(pedidos).registrarTramo(eq(pedido),km.capture(),eq(180L),any());
        assertThat(km.getValue().doubleValue()).isCloseTo(1.112,within(0.002));
    }
    @Test void elRuidoDelGpsConElPilotoDetenidoNoSuma() {
        publicar(14.60,-90.49);
        reloj.avanzar(Duration.ofSeconds(5));
        publicar(14.60003,-90.49); // ~3 m
        verify(pedidos,never()).registrarTramo(any(),any(),anyLong(),any());
    }
    @Test void losPequenosMovimientosCuentanCuandoSumanDiezMetros() {
        publicar(14.60,-90.49);
        reloj.avanzar(Duration.ofSeconds(5));
        publicar(14.60005,-90.49); // ~5.6 m: aún no cuenta
        reloj.avanzar(Duration.ofSeconds(5));
        publicar(14.60010,-90.49); // ~11 m desde el inicio
        verify(pedidos,times(1)).registrarTramo(eq(pedido),any(),anyLong(),any());
    }
    @Test void unSaltoImposibleNoSeCuentaYElRecorridoSigueDesdeAhi() {
        publicar(14.60,-90.49);
        reloj.avanzar(Duration.ofSeconds(2));
        publicar(14.70,-90.49); // 11 km en 2 s: lectura mala
        verify(pedidos,never()).registrarTramo(any(),any(),anyLong(),any());
        reloj.avanzar(Duration.ofMinutes(3));
        publicar(14.71,-90.49); // desde la lectura nueva: ~1.1 km normales
        verify(pedidos,times(1)).registrarTramo(eq(pedido),any(),anyLong(),any());
    }
    @Test void laVelocidadSeMideSobreLosUltimosSegundos() {
        assertThat(publicar(14.60,-90.49).velocidad()).isNull(); // al arrancar no hay con qué comparar
        reloj.avanzar(Duration.ofSeconds(2));
        assertThat(publicar(14.60005,-90.49).velocidad()).isNull(); // menos de 4 s de historia
        reloj.avanzar(Duration.ofSeconds(10));
        // 0.001° de latitud (~111 m) en 12 s ≈ 33 km/h
        assertThat(publicar(14.601,-90.49).velocidad()).isCloseTo(33.4,within(0.5));
    }
    @Test void detenidoLaVelocidadEsCasiCero() {
        publicar(14.60,-90.49);
        for (int i=0;i<6;i++) { reloj.avanzar(Duration.ofSeconds(5)); publicar(14.60+(i%2)*0.00002,-90.49); } // ruido de ~2 m
        var u=servicio.ultima(pedido).orElseThrow();
        assertThat(u.velocidad()).isLessThan(1.0);
        assertThat(u.detenidoDesde()).isNotNull().isBefore(u.marca());
        // Al volver a moverse deja de estar detenido
        reloj.avanzar(Duration.ofSeconds(10));
        assertThat(publicar(14.602,-90.49).detenidoDesde()).isNull();
    }
    @Test void trasPerderLaSenalLaVelocidadSeReinicia() {
        publicar(14.60,-90.49);
        reloj.avanzar(Duration.ofSeconds(10));
        assertThat(publicar(14.601,-90.49).velocidad()).isNotNull();
        reloj.avanzar(Duration.ofMinutes(2));
        assertThat(publicar(14.602,-90.49).velocidad()).isNull();
    }
    @Test void unTramoTrasMuchoTiempoSinSenalSumaDistanciaPeroNoTiempo() {
        publicar(14.60,-90.49);
        reloj.avanzar(Duration.ofMinutes(20));
        publicar(14.61,-90.49);
        verify(pedidos).registrarTramo(eq(pedido),any(),eq(0L),any());
    }
}
