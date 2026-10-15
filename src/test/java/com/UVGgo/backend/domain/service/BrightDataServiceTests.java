package com.UVGgo.backend.domain.service;

import com.UVGgo.backend.domain.ProductoBrightData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrightDataServiceTests {
    private static final String URL_RUN = "https://api.brightdata.com/dca/dataset?id=j_prueba";
    private MockRestServiceServer servidor;
    private BrightDataService service;

    @BeforeEach
    void configurar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.brightdata.com");
        servidor = MockRestServiceServer.bindTo(builder).build();
        service = new BrightDataService(builder.build(), new JsonMapper(), "clave-prueba");
    }

    @Test
    void leeNdjsonDeUnRun() {
        servidor.expect(requestTo(URL_RUN))
                .andExpect(header("Authorization", "Bearer clave-prueba"))
                .andRespond(withSuccess("""
                        {"product_name":"R-PICO Raspberry PI Pico","price":{"value":55,"currency":"GTQ","symbol":"Q"},"currency":"Q","product_page_url":"https://electronicarych.com/shop/r-pico-1"}
                        {"product_name":"PL-343BL Plug Banana","price":{"value":11.5,"currency":"GTQ","symbol":"Q"},"product_page_url":"https://electronicarych.com/shop/pl-343bl-2"}
                        """, MediaType.APPLICATION_JSON));

        List<ProductoBrightData> productos = service.descargarRun("j_prueba");

        assertThat(productos).hasSize(2);
        assertThat(productos.getFirst().titulo()).isEqualTo("R-PICO Raspberry PI Pico");
        assertThat(productos.getFirst().url()).isEqualTo("https://electronicarych.com/shop/r-pico-1");
        assertThat(productos.getFirst().precio()).isEqualTo(Map.of("value", 55, "currency", "GTQ", "symbol", "Q"));
        servidor.verify();
    }

    @Test
    void avisaCuandoElRunTodaviaSePrepara() {
        servidor.expect(requestTo(URL_RUN)).andRespond(withStatus(HttpStatus.ACCEPTED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"building\",\"message\":\"Dataset is not ready yet, try again in 30s\"}"));

        assertThatThrownBy(() -> service.descargarRun("j_prueba"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("todavía está preparando");
    }

    @Test
    void rechazaSinApiKey() {
        BrightDataService sinClave = new BrightDataService(RestClient.create(), new JsonMapper(), "");
        assertThatThrownBy(() -> sinClave.descargarRun("j_prueba"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("API_KEY_BRIGHTDATA");
    }
}
