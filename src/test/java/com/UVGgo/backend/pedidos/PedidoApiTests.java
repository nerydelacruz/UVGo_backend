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
import org.springframework.test.web.servlet.ResultActions;
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
                .andExpect(jsonPath("$.estado").value("SOLICITADO"))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andExpect(jsonPath("$.fechaCancelacion").doesNotExist())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode crear() throws Exception {
        // ID declarado que no necesita existir en el stock.
        return crear("[{\"articuloId\":900002,\"cantidad\":2.125}]");
    }

    @Test
    void creaYConsultaSinDependerDelStock() throws Exception {
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
                .andExpect(jsonPath("$.detalles[0].articuloId").value(900002))
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
                EstadoPedido.SOLICITADO, EstadoPedido.CANCELADO)).isEqualTo(1);
        assertThat(pedidos.cancelarPendiente(id, primera.plusSeconds(60),
                EstadoPedido.SOLICITADO, EstadoPedido.CANCELADO)).isZero();
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
                datos(0, "1"), datos(-2, "1"), datos(2, "null"),
                datos(2, "0"), datos(2, "-1"), datos(2, "1.0001"),
                datos(2, "1000000000"),
                "{\"detalles\":[{\"cantidad\":1}]}",
                "{\"detalles\":[{\"articuloId\":2}]}"
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
                  {"articuloId":2,"cantidad":1},
                  {"articuloId":2,"cantidad":3}
                ]}
                """))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero();
    }

    @Test
    void permiteVariosArticulosYConservaOrdenYCantidadesLimite() throws Exception {
        JsonNode pedido = crear("""
                [{"articuloId":1,"cantidad":0.001},
                 {"articuloId":3,"cantidad":999999999.999}]
                """);
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/pedidos/" + pedido.get("pedidoId").asInt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles.length()").value(2))
                .andExpect(jsonPath("$.detalles[0].articuloId").value(1))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(0.001))
                .andExpect(jsonPath("$.detalles[1].articuloId").value(3))
                .andExpect(jsonPath("$.detalles[1].cantidad").value(999999999.999));
    }

    @Test
    void clienteNoPuedeAsignarEstadoPreciosNiIdentidad() throws Exception {
        var resultado = mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content("""
                {"pedidoId":700,"estado":"CANCELADO","total":1,
                 "fechaCreacion":"2000-01-01T00:00:00Z","fechaCancelacion":"2000-01-01T00:00:00Z",
                 "detalles":[{"id":700,"articuloId":2,"cantidad":1,"precio":1}]}
                """)).andReturn().getResponse();
        // Aceptar solo campos conocidos o rechazar campos extra son ambas respuestas seguras.
        if (resultado.getStatus() == 400) {
            assertThat(pedidos.count()).isZero();
        } else {
            assertThat(resultado.getStatus()).isEqualTo(201);
            JsonNode pedido = json.readTree(resultado.getContentAsString());
            assertThat(pedido.get("estado").asText()).isEqualTo("SOLICITADO");
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

    @Test
    void consultaEstadoConSiguientePermitido() throws Exception {
        int id = crear().get("pedidoId").asInt();
        mvc.perform(get("/pedidos/" + id + "/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidoId").value(id))
                .andExpect(jsonPath("$.estado").value("SOLICITADO"))
                .andExpect(jsonPath("$.siguienteEstado").value("REVISION"))
                .andExpect(jsonPath("$.anteriorEstado").doesNotExist());
    }

    @Test
    void avanzaPorTodoElFlujoHastaEntregado() throws Exception {
        JsonNode original = crear();
        int id = original.get("pedidoId").asInt();
        for (String estado : new String[]{"REVISION", "COTIZADO", "CONFIRMADO", "EN_RUTA", "ENTREGADO"}) {
            entityManager.flush();
            entityManager.clear();
            JsonNode actualizado = json.readTree(actualizarEstado(id, estado)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value(estado))
                    .andExpect(jsonPath("$.fechaCancelacion").doesNotExist())
                    .andReturn().getResponse().getContentAsString());
            assertThat(actualizado.get("detalles")).isEqualTo(original.get("detalles"));
        }
        mvc.perform(get("/pedidos/" + id + "/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"))
                .andExpect(jsonPath("$.siguienteEstado").doesNotExist());
        actualizarEstado(id, "SOLICITADO").andExpect(status().isConflict());
    }

    @Test
    void repetirElMismoEstadoNoFalla() throws Exception {
        int id = crear().get("pedidoId").asInt();
        actualizarEstado(id, "REVISION").andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        actualizarEstado(id, "REVISION").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REVISION"));
    }

    @Test
    void rechazaSaltosYCancelados() throws Exception {
        int id = crear().get("pedidoId").asInt();
        actualizarEstado(id, "COTIZADO").andExpect(status().isConflict());
        actualizarEstado(id, "ENTREGADO").andExpect(status().isConflict());
        actualizarEstado(id, "REVISION").andExpect(status().isOk());
        actualizarEstado(id, "COTIZADO").andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        // Retroceder más de un paso también es un salto.
        actualizarEstado(id, "SOLICITADO").andExpect(status().isConflict());

        int cancelado = crear().get("pedidoId").asInt();
        mvc.perform(post("/pedidos/" + cancelado + "/cancelar")).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        actualizarEstado(cancelado, "REVISION").andExpect(status().isConflict());
        actualizarEstado(cancelado, "SOLICITADO").andExpect(status().isConflict());
        mvc.perform(get("/pedidos/" + cancelado + "/estado"))
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.siguienteEstado").doesNotExist())
                .andExpect(jsonPath("$.anteriorEstado").doesNotExist());
    }

    @Test
    void retrocedeUnPasoHastaSolicitado() throws Exception {
        int id = crear().get("pedidoId").asInt();
        actualizarEstado(id, "SOLICITADO").andExpect(status().isOk());
        for (String estado : new String[]{"REVISION", "COTIZADO", "CONFIRMADO", "EN_RUTA", "ENTREGADO"}) {
            actualizarEstado(id, estado).andExpect(status().isOk());
            entityManager.flush();
            entityManager.clear();
        }
        mvc.perform(get("/pedidos/" + id + "/estado"))
                .andExpect(jsonPath("$.anteriorEstado").value("EN_RUTA"));
        for (String estado : new String[]{"EN_RUTA", "CONFIRMADO", "COTIZADO", "REVISION", "SOLICITADO"}) {
            actualizarEstado(id, estado).andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value(estado));
            entityManager.flush();
            entityManager.clear();
        }
        mvc.perform(get("/pedidos/" + id + "/estado"))
                .andExpect(jsonPath("$.estado").value("SOLICITADO"))
                .andExpect(jsonPath("$.anteriorEstado").doesNotExist());
        // De vuelta en SOLICITADO se puede cancelar.
        mvc.perform(post("/pedidos/" + id + "/cancelar")).andExpect(status().isOk());
    }

    @Test
    void rechazaEstadosInvalidosYCancelarPorEstaRuta() throws Exception {
        int id = crear().get("pedidoId").asInt();
        for (String body : new String[]{"{}", "{\"estado\":null}", "{\"estado\":\"INVENTADO\"}"}) {
            mvc.perform(put("/pedidos/" + id + "/estado").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        actualizarEstado(id, "CANCELADO").andExpect(status().isBadRequest());
        mvc.perform(get("/pedidos/" + id)).andExpect(jsonPath("$.estado").value("SOLICITADO"));
    }

    @Test
    void actualizacionCondicionalNoSobrescribeOtroCambio() throws Exception {
        int id = crear().get("pedidoId").asInt();
        assertThat(pedidos.actualizarEstado(id, EstadoPedido.SOLICITADO, EstadoPedido.REVISION)).isEqualTo(1);
        assertThat(pedidos.actualizarEstado(id, EstadoPedido.SOLICITADO, EstadoPedido.REVISION)).isZero();
        assertThat(pedidos.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoPedido.REVISION);
    }

    @Test
    void detectaArticulosUsadosEnPedidos() throws Exception {
        crear();
        assertThat(pedidos.contieneArticulo(900002)).isTrue();
        assertThat(pedidos.contieneArticulo(900003)).isFalse();
    }

    @Test
    void estadoDePedidoInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/pedidos/2147483647/estado")).andExpect(status().isNotFound());
        actualizarEstado(2147483647, "REVISION").andExpect(status().isNotFound());
    }

    private ResultActions actualizarEstado(int id, String estado) throws Exception {
        return mvc.perform(put("/pedidos/" + id + "/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"" + estado + "\"}"));
    }

    private String datos(int articuloId, String cantidad) {
        return "{\"detalles\":[{\"articuloId\":%d,\"cantidad\":%s}]}"
                .formatted(articuloId, cantidad);
    }
}
