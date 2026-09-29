package com.UVGgo.backend.pedidos;

import com.UVGgo.backend.domain.pedidos.EstadoPedido;
import com.UVGgo.backend.persistence.crud.pedidos.PedidoCrudRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PedidoApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PedidoCrudRepository pedidos;
    @Autowired EntityManager entityManager;

    private JsonNode crear(String detalles) throws Exception {
        return json.readTree(mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"detalles\":" + detalles + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_COTIZACION"))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andExpect(jsonPath("$.fechaCancelacion").doesNotExist())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode crear() throws Exception {
        // IDs declarados que no necesitan existir en las tablas de kits/artículos.
        return crear("[{\"kitId\":900001,\"kitArticuloId\":900002,\"cantidad\":2.125}]");
    }

    @Test
    void creaYConsultaSinDependerDeKits() throws Exception {
        JsonNode pedido = crear();
        int id = pedido.get("pedidoId").asInt();
        assertThat(id).isPositive();
        assertThat(pedido.get("detalles").get(0).get("id").asInt()).isPositive();
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/pedidos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidoId").value(id))
                .andExpect(jsonPath("$.detalles.length()").value(1))
                .andExpect(jsonPath("$.detalles[0].kitId").value(900001))
                .andExpect(jsonPath("$.detalles[0].kitArticuloId").value(900002))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(2.125));
    }

    @Test
    void listaDelMasRecienteAlMasAntiguo() throws Exception {
        int primero = crear().get("pedidoId").asInt();
        int segundo = crear().get("pedidoId").asInt();
        var fecha = Instant.parse("2026-01-01T00:00:00Z");
        // Fechas iguales verifican el desempate por ID sin depender del reloj.
        entityManager.createQuery("update PedidoEntity p set p.fechaCreacion = :fecha")
                .setParameter("fecha", fecha).executeUpdate();
        entityManager.clear();
        mvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].pedidoId").value(segundo))
                .andExpect(jsonPath("$[1].pedidoId").value(primero));
    }

    @Test
    void listaVacia() throws Exception {
        mvc.perform(get("/pedidos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cancelaSinPerderDetallesYConservaFechaAlRepetir() throws Exception {
        JsonNode original = crear();
        int id = original.get("pedidoId").asInt();
        entityManager.flush();
        entityManager.clear();
        JsonNode cancelado = json.readTree(mvc.perform(post("/pedidos/" + id + "/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.fechaCancelacion").isNotEmpty())
                .andReturn().getResponse().getContentAsString());
        assertThat(cancelado.get("detalles")).isEqualTo(original.get("detalles"));
        entityManager.flush();
        entityManager.clear();
        JsonNode repetido = json.readTree(mvc.perform(post("/pedidos/" + id + "/cancelar"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(Instant.parse(repetido.get("fechaCancelacion").asText()))
                .isEqualTo(Instant.parse(cancelado.get("fechaCancelacion").asText()));
        assertThat(Instant.parse(repetido.get("fechaCreacion").asText()))
                .isEqualTo(Instant.parse(cancelado.get("fechaCreacion").asText()));
        assertThat(repetido.get("detalles")).isEqualTo(cancelado.get("detalles"));
        mvc.perform(get("/pedidos/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    @Test
    void cancelacionCondicionalNoSobrescribeUnaCancelacionAnterior() throws Exception {
        int id = crear().get("pedidoId").asInt();
        Instant primera = Instant.parse("2026-01-01T01:00:00Z");
        assertThat(pedidos.cancelarPendiente(id, primera,
                EstadoPedido.PENDIENTE_COTIZACION, EstadoPedido.CANCELADO)).isEqualTo(1);
        assertThat(pedidos.cancelarPendiente(id, primera.plusSeconds(60),
                EstadoPedido.PENDIENTE_COTIZACION, EstadoPedido.CANCELADO)).isZero();
        assertThat(pedidos.findById(id).orElseThrow().getFechaCancelacion()).isEqualTo(primera);
    }

    @Test
    void devuelve404ParaPedidosInexistentes() throws Exception {
        mvc.perform(get("/pedidos/2147483647")).andExpect(status().isNotFound());
        mvc.perform(post("/pedidos/2147483647/cancelar")).andExpect(status().isNotFound());
    }

    @Test
    void rechazaDatosInvalidosSinGuardarPedidosNiDetalles() throws Exception {
        for (String body : new String[]{
                "{}", "{\"detalles\":null}", "{\"detalles\":[]}", "{\"detalles\":[null]}",
                "{\"detalles\":[{}]}",
                datos(0, 2, "1"), datos(1, -2, "1"), datos(1, 2, "null"),
                datos(1, 2, "0"), datos(1, 2, "-1"), datos(1, 2, "1.0001"),
                datos(1, 2, "1000000000"),
                "{\"detalles\":[{\"kitId\":1,\"cantidad\":1}]}",
                "{\"detalles\":[{\"kitArticuloId\":2,\"cantidad\":1}]}"
        }) {
            mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertThat(pedidos.count()).isZero();
        assertThat(entityManager.createQuery("select count(d) from PedidoDetalleEntity d", Long.class)
                .getSingleResult()).isZero();
    }

    @Test
    void rechazaReferenciasDuplicadas() throws Exception {
        mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content("""
                {"detalles":[
                  {"kitId":1,"kitArticuloId":2,"cantidad":1},
                  {"kitId":1,"kitArticuloId":2,"cantidad":3}
                ]}
                """))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero();
    }

    @Test
    void permiteVariosKitsYConservaOrdenYCantidadesLimite() throws Exception {
        JsonNode pedido = crear("""
                [{"kitId":1,"kitArticuloId":2,"cantidad":0.001},
                 {"kitId":3,"kitArticuloId":2,"cantidad":999999999.999}]
                """);
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/pedidos/" + pedido.get("pedidoId").asInt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles.length()").value(2))
                .andExpect(jsonPath("$.detalles[0].kitId").value(1))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(0.001))
                .andExpect(jsonPath("$.detalles[1].kitId").value(3))
                .andExpect(jsonPath("$.detalles[1].cantidad").value(999999999.999));
    }

    @Test
    void clienteNoPuedeAsignarEstadoPreciosNiIdentidad() throws Exception {
        var resultado = mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content("""
                {"pedidoId":700,"estado":"CANCELADO","total":1,
                 "fechaCreacion":"2000-01-01T00:00:00Z","fechaCancelacion":"2000-01-01T00:00:00Z",
                 "detalles":[{"id":700,"kitId":1,"kitArticuloId":2,"cantidad":1,"precio":1}]}
                """)).andReturn().getResponse();
        // Aceptar solo campos conocidos o rechazar campos extra son ambas respuestas seguras.
        if (resultado.getStatus() == 400) {
            assertThat(pedidos.count()).isZero();
        } else {
            assertThat(resultado.getStatus()).isEqualTo(201);
            JsonNode pedido = json.readTree(resultado.getContentAsString());
            assertThat(pedido.get("estado").asText()).isEqualTo("PENDIENTE_COTIZACION");
            assertThat(pedido.get("pedidoId").asInt()).isNotEqualTo(700);
            assertThat(pedido.get("detalles").get(0).get("id").asInt()).isNotEqualTo(700);
            assertThat(Instant.parse(pedido.get("fechaCreacion").asText()))
                    .isAfter(Instant.parse("2000-01-01T00:00:00Z"));
            assertThat(pedido.path("fechaCancelacion").isNull() || pedido.path("fechaCancelacion").isMissingNode()).isTrue();
            assertThat(pedido.has("total")).isFalse();
            assertThat(pedido.get("detalles").get(0).has("precio")).isFalse();
        }
    }

    @Test
    void noExponeEdicionNiEliminacionFisica() throws Exception {
        int id = crear().get("pedidoId").asInt();
        mvc.perform(delete("/pedidos/" + id)).andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/pedidos/" + id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        assertThat(pedidos.existsById(id)).isTrue();
    }

    private String datos(int kitId, int articuloId, String cantidad) {
        return "{\"detalles\":[{\"kitId\":%d,\"kitArticuloId\":%d,\"cantidad\":%s}]}"
                .formatted(kitId, articuloId, cantidad);
    }
}
