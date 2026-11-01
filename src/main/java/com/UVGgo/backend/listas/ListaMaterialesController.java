package com.UVGgo.backend.listas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import static com.UVGgo.backend.listas.ListaMaterialesDtos.*;

@RestController
@Validated
@RequestMapping("/api/v1/listas-materiales")
public class ListaMaterialesController {
    private final ListaMaterialesRepository listas;
    private final ObjectMapper json;
    public ListaMaterialesController(ListaMaterialesRepository listas, ObjectMapper json) {
        this.listas = listas; this.json = json;
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Detalle crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Crear solicitud) {
        var lista = listas.save(new ListaMateriales(UUID.fromString(jwt.getSubject()), solicitud.nombre(),
                json.writeValueAsString(solicitud.materiales())));
        return detalle(lista);
    }
    @GetMapping
    public Pagina listar(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue="0") @Min(0) int pagina) {
        var resultado = listas.findByUsuarioIdOrderByCreadaDesc(UUID.fromString(jwt.getSubject()), PageRequest.of(pagina,20));
        return new Pagina(resultado.map(l -> new Resumen(l.getId(), l.getNombre(), l.getCreada())).getContent(), resultado.hasNext());
    }
    @GetMapping("/{id}")
    public Detalle consultar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return detalle(listas.findByIdAndUsuarioId(id, UUID.fromString(jwt.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lista no encontrada")));
    }
    private Detalle detalle(ListaMateriales lista) {
        return new Detalle(lista.getId(), lista.getNombre(), lista.getCreada(),
                json.readValue(lista.getMateriales(), new TypeReference<List<Material>>() {}), lista.getSolicitudId());
    }
}
