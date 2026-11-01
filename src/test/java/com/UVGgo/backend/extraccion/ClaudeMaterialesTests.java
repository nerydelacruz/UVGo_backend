package com.UVGgo.backend.extraccion;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ClaudeMaterialesTests {
    private final JsonMapper json = new JsonMapper();
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://api.anthropic.com");
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
    private final ClaudeMateriales proveedor = new ClaudeMateriales(json, builder.build(), true, "clave-de-prueba", "claude-haiku-4-5");
    private final String fila = """
        {"nombre":"Guantes","descripcion":null,"cantidad":2,"unidad":"par",
         "observaciones":null,"referencia":"Dos pares de guantes"}
        """;

    private String respuesta(String filas) {
        return json.writeValueAsString(Map.of("stop_reason", "end_turn", "content", List.of(Map.of(
                "type", "text", "text", "{\"excedeLimite\":false,\"materiales\":[" + filas + "]}"))));
    }

    @Test void enviaContratoOficialYConservaCantidadYUnidad() {
        servidor.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(header("x-api-key", "clave-de-prueba"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(jsonPath("$.model").value("claude-haiku-4-5"))
                .andExpect(jsonPath("$.output_config.format.type").value("json_schema"))
                .andExpect(jsonPath("$.messages[0].content").value("Dos pares de guantes"))
                .andRespond(withSuccess(respuesta(fila), MediaType.APPLICATION_JSON));
        var material = proveedor.extraer("Dos pares de guantes").getFirst();
        assertThat(material.cantidad()).isEqualByComparingTo("2");
        assertThat(material.unidad()).isEqualTo("par");
        assertThat(material.descripcion()).isNull();
        assertThat(material.advertencias()).containsExactly("Falta completar la descripción.");
        servidor.verify();
    }

    @Test void noInventaCantidadesAusentesNiAceptaNegativas() {
        for (String valor : List.of("null", "-2", "0", "0.0001", "1000000000")) {
            var material = proveedor.interpretar(respuesta(fila.replace("\"cantidad\":2", "\"cantidad\":" + valor)), "Dos pares de guantes").getFirst();
            assertThat(material.cantidad()).isNull();
            assertThat(material.advertencias()).contains("Falta confirmar la cantidad.");
        }
    }

    @Test void advierteReferenciasNoVerificables() {
        assertThat(proveedor.interpretar(respuesta(fila), "Un cuaderno").getFirst().advertencias())
                .anyMatch(texto -> texto.contains("no coincide"));
    }

    @Test void rechazaTruncamientoRechazoYJsonInvalido() {
        for (String dato : List.of("no json", "{}", respuesta(fila).replace("end_turn", "max_tokens"),
                respuesta(fila).replace("end_turn", "refusal"), respuesta(fila.replace("Guantes", "a".repeat(121))))) {
            assertThatThrownBy(() -> proveedor.interpretar(dato, "Dos pares de guantes"))
                    .isInstanceOf(ResponseStatusException.class);
        }
        assertThat(proveedor.interpretar(respuesta(""), "Sin materiales")).isEmpty();
    }

    @Test void noAceptaResultadoParcialPorLimite() {
        assertThatThrownBy(() -> proveedor.interpretar(respuesta(fila).replace("false", "true"), "Texto"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("50 materiales");
    }

    @Test void sinClaveNoLlamaAlProveedor() {
        var sinClave = new ClaudeMateriales(json, builder.build(), true, "", "claude-haiku-4-5");
        assertThatThrownBy(() -> sinClave.extraer("Texto de prueba"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("no está configurada");
        servidor.verify();
    }

    @Test void erroresDelProveedorNoFiltranCuerpoNiReintentan() {
        for (HttpStatus status : List.of(HttpStatus.UNAUTHORIZED, HttpStatus.BAD_REQUEST, HttpStatus.TOO_MANY_REQUESTS, HttpStatus.INTERNAL_SERVER_ERROR)) {
            servidor.reset();
            servidor.expect(requestTo("https://api.anthropic.com/v1/messages"))
                    .andRespond(withStatus(status).body("contenido privado del documento"));
            assertThatThrownBy(() -> proveedor.extraer("Texto"))
                    .isInstanceOf(ResponseStatusException.class).hasMessageNotContaining("contenido privado");
            servidor.verify();
        }
    }
}
