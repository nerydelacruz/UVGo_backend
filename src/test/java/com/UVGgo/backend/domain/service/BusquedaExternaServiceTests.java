package com.UVGgo.backend.domain.service;

import com.UVGgo.backend.domain.ProductoExterno;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BusquedaExternaServiceTests {
    @Test
    void devuelveFichaDeGooglePrecioYMoneda() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://serpapi.com");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        BusquedaExternaService service = new BusquedaExternaService(builder.build(), "clave-prueba", "es", "us");
        servidor.expect(queryParam("q", "calculadora"))
                .andRespond(withSuccess("""
                        {"shopping_results":[
                          {"title":"Casio fx-991","thumbnail":"https://img.example/1.jpg","source":"Walmart",
                           "price":"$19.99","extracted_price":19.99,
                           "product_link":"https://www.google.com/search?ibp=oshop&q=casio&prds=catalogid:1"},
                          {"title":"Sin precio","source":"Tienda"}]}
                        """, MediaType.APPLICATION_JSON));

        List<ProductoExterno> productos = service.buscar(" calculadora ");

        assertThat(productos.getFirst()).isEqualTo(new ProductoExterno("Casio fx-991",
                "https://www.google.com/search?ibp=oshop&q=casio&prds=catalogid:1", "https://img.example/1.jpg",
                "Walmart", new BigDecimal("19.99"), "USD", "Walmart · $19.99"));
        assertThat(productos.get(1).url()).isNull();
        assertThat(productos.get(1).precio()).isNull();
        servidor.verify();
    }
}
