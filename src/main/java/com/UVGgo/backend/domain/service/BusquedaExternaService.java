package com.UVGgo.backend.domain.service;

import com.UVGgo.backend.domain.ProductoExterno;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BusquedaExternaService {
    private final RestClient serpApi = RestClient.create("https://serpapi.com");

    @Value("${serpapi.api-key:}")
    private String apiKey;
    @Value("${serpapi.hl:es}")
    private String idioma;
    @Value("${serpapi.gl:us}")
    private String pais;

    public List<ProductoExterno> buscar(String consulta) {
        if (consulta == null || consulta.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El parámetro q es obligatorio");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Falta configurar API_KEY_SERPAPI");
        }

        RespuestaSerpApi respuesta;
        try {
            respuesta = serpApi.get()
                    .uri(uri -> uri.path("/search.json")
                            .queryParam("engine", "google_shopping")
                            .queryParam("q", consulta.trim())
                            .queryParam("hl", idioma)
                            .queryParam("gl", pais)
                            .queryParam("api_key", apiKey)
                            .build())
                    .retrieve()
                    .body(RespuestaSerpApi.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo consultar SerpApi");
        }

        if (respuesta == null || respuesta.resultados() == null) {
            // SerpApi responde con "error" cuando no hay resultados para la búsqueda.
            return List.of();
        }
        return respuesta.resultados().stream()
                .map(r -> new ProductoExterno(r.title(), r.thumbnail(), descripcion(r)))
                .toList();
    }

    // Google Shopping no trae descripción; se arma con la tienda y el precio.
    private String descripcion(ResultadoSerpApi r) {
        return Stream.of(r.source(), r.price())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" · "));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaSerpApi(@JsonProperty("shopping_results") List<ResultadoSerpApi> resultados) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResultadoSerpApi(String title, String thumbnail, String source, String price) {
    }
}
