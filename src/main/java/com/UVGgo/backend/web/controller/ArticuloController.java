package com.UVGgo.backend.web.controller;

import com.UVGgo.backend.domain.Articulo;
import com.UVGgo.backend.domain.service.KitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/kits/{kitId}/articulos")
public class ArticuloController {
    private final KitService kits;

    public ArticuloController(KitService kits) { this.kits = kits; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Articulo agregarArticulo(@PathVariable int kitId, @Valid @RequestBody Articulo articulo) {
        return kits.agregarArticulo(kitId, articulo);
    }

    @PutMapping("/{articuloId}")
    public Articulo editarArticulo(@PathVariable int kitId, @PathVariable int articuloId,
                                  @Valid @RequestBody Articulo articulo) {
        return kits.editarArticulo(kitId, articuloId, articulo);
    }

    @DeleteMapping("/{articuloId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirarArticulo(@PathVariable int kitId, @PathVariable int articuloId) {
        kits.retirarArticulo(kitId, articuloId);
    }

    @GetMapping
    public List<Articulo> listarArticulos(@PathVariable int kitId) {
        return kits.listarArticulos(kitId);
    }
}
