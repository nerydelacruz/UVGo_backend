package com.UVGgo.backend.domain.service;

import com.UVGgo.backend.domain.ProductoBrightData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class BrightDataService {
    private final RestClient brightData;
    private final ObjectMapper json;
    private final String apiKey;

    @Autowired
    public BrightDataService(ObjectMapper json, @Value("${brightdata.api-key:}") String apiKey) {
        this(RestClient.create("https://api.brightdata.com"), json, apiKey);
    }

    BrightDataService(RestClient brightData, ObjectMapper json, String apiKey) {
        this.brightData = brightData;
        this.json = json;
        this.apiKey = apiKey;
    }

    // Descarga los resultados de un run de Scraper Studio (el ID "j_..." de la pestaña Runs).
    public List<ProductoBrightData> descargarRun(String runId) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Falta configurar API_KEY_BRIGHTDATA");
        }
        ResponseEntity<String> respuesta;
        try {
            respuesta = brightData.get()
                    .uri(uri -> uri.path("/dca/dataset").queryParam("id", runId).build())
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .toEntity(String.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo consultar BrightData");
        }
        // La primera vez que se pide un run, BrightData responde 202 mientras arma el archivo.
        if (respuesta.getStatusCode().value() == HttpStatus.ACCEPTED.value()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "BrightData todavía está preparando los datos del run; intente de nuevo en 30 segundos");
        }
        return leerRegistros(respuesta.getBody());
    }

    // BrightData entrega NDJSON (un producto por línea), aunque algunos formatos son un arreglo JSON.
    private List<ProductoBrightData> leerRegistros(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) return List.of();
        try {
            String texto = cuerpo.strip();
            if (texto.startsWith("[")) {
                return List.of(json.readValue(texto, ProductoBrightData[].class));
            }
            List<ProductoBrightData> productos = new ArrayList<>();
            for (String linea : texto.split("\\R")) {
                if (!linea.isBlank()) productos.add(json.readValue(linea, ProductoBrightData.class));
            }
            return productos;
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "BrightData devolvió datos con un formato inesperado");
        }
    }
}
