package com.UVGgo.backend.pedido;

import static org.assertj.core.api.Assertions.assertThat;

import com.UVGgo.backend.domain.Articulo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoincidenciaArticulosTests {
    private static Articulo articulo(int id,String nombre,String descripcion) {
        return new Articulo(id,nombre,descripcion,null,null,new BigDecimal("5.00"),"GTQ","Tienda",null,null);
    }
    private final CoincidenciaArticulos catalogo=new CoincidenciaArticulos(List.of(
            articulo(1,"Circuito integrado 74LS08","Compuerta AND de 2 entradas x4 DIP-14"),
            articulo(2,"Resistencia 10K ohm 1/4W","Resistencia de carbón 10kΩ"),
            articulo(3,"Resistencia 100K ohm 1/4W","Resistencia de carbón 100kΩ"),
            articulo(4,"Módulo MB102 para protoboard","Fuente de alimentación 3.3V/5V"),
            articulo(5,"Temporizador NE555N","Integrado 555 DIP-8")));

    @Test void enlazaPorCodigoDeParteIgnorandoTildesYMayusculas() {
        var r=catalogo.buscar("Módulo de alimentación MB102 para protoboard",null,null);
        assertThat(r).hasValueSatisfying(x -> { assertThat(x.articulo().id()).isEqualTo(4); assertThat(x.segura()).isTrue(); });
    }
    @Test void usaElCodigoQueSugiereClaude() {
        var r=catalogo.buscar("Integrado temporizador","ne555n",null);
        assertThat(r).hasValueSatisfying(x -> { assertThat(x.articulo().id()).isEqualTo(5); assertThat(x.segura()).isTrue(); });
    }
    @Test void noConfundeValoresDeResistencias() {
        var r=catalogo.buscar("Resistencias 10 kΩ 1/4W",null,"resistencia 10k ohm 1/4W");
        assertThat(r).hasValueSatisfying(x -> assertThat(x.articulo().id()).isEqualTo(2));
        var otra=catalogo.buscar("Resistencia 220 ohm 1/4W",null,null);
        assertThat(otra.map(x -> x.segura()).orElse(false)).isFalse();
    }
    @Test void sinParecidoNoDevuelveNada() {
        assertThat(catalogo.buscar("Pulsador normalmente abierto 6x6x5mm",null,null)).isEmpty();
    }
    @Test void normalizaUnidades() {
        assertThat(CoincidenciaArticulos.normalizar("10kΩ")).isEqualTo("10 k ohm");
        assertThat(CoincidenciaArticulos.normalizar("Módulo 5V")).isEqualTo("modulo 5 v");
    }
}
