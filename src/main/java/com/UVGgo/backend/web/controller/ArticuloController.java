package com.UVGgo.backend.web.controller;

import com.UVGgo.backend.domain.Articulo;
import com.UVGgo.backend.domain.ProductoBrightData;
import com.UVGgo.backend.domain.ResultadoImportacion;
import com.UVGgo.backend.domain.service.ArticuloService;
import com.UVGgo.backend.domain.service.BrightDataService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/articulos")
public class ArticuloController {
    @Autowired
    private ArticuloService articuloService;
    @Autowired
    private BrightDataService brightDataService;

    @GetMapping
    public List<Articulo> listar(@RequestParam(required = false) String q,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 @RequestParam(defaultValue = "50") int tamano) {
        return articuloService.listar(q, pagina, tamano);
    }

    @GetMapping("/{articuloId}")
    public Articulo consultar(@PathVariable int articuloId) {
        return articuloService.consultar(articuloId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Articulo agregar(@Valid @RequestBody Articulo articulo) {
        return articuloService.agregar(articulo);
    }

    @PutMapping("/{articuloId}")
    public Articulo editar(@PathVariable int articuloId, @Valid @RequestBody Articulo articulo) {
        return articuloService.editar(articuloId, articulo);
    }

    @DeleteMapping("/{articuloId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable int articuloId) {
        articuloService.eliminar(articuloId);
    }

    // Recibe el arreglo JSON exportado de un dataset de BrightData.
    @PostMapping("/importar/brightdata")
    public ResultadoImportacion importarBrightData(@RequestBody List<ProductoBrightData> productos) {
        return articuloService.importarBrightData(productos);
    }

    // Trae los productos de un run de Scraper Studio directamente desde la API de BrightData.
    @PostMapping("/importar/brightdata/{runId}")
    public ResultadoImportacion importarRunBrightData(@PathVariable String runId) {
        return articuloService.importarBrightData(brightDataService.descargarRun(runId));
    }
}
