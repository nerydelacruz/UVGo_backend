package com.UVGgo.backend.web.controller;

import com.UVGgo.backend.domain.ProductoExterno;
import com.UVGgo.backend.domain.service.BusquedaExternaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/busqueda-externa")
public class BusquedaExternaController {
    private final BusquedaExternaService busquedaExterna;

    public BusquedaExternaController(BusquedaExternaService busquedaExterna) {
        this.busquedaExterna = busquedaExterna;
    }

    @GetMapping
    public List<ProductoExterno> buscar(@RequestParam(required = false) String q) {
        return busquedaExterna.buscar(q);
    }
}
