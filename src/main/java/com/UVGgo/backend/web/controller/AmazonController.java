package com.UVGgo.backend.web.controller;

import com.UVGgo.backend.domain.ProductoAmazon;
import com.UVGgo.backend.domain.service.AmazonProductService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/amazon")
public class AmazonController {
    private final AmazonProductService amazon;

    public AmazonController(AmazonProductService amazon) { this.amazon = amazon; }

    @GetMapping("/buscar")
    public List<ProductoAmazon> buscar(@RequestParam String q,
                                       @RequestParam(defaultValue = "10") int cantidad) {
        return amazon.buscar(q, Math.min(Math.max(cantidad, 1), 10));
    }

    @GetMapping("/productos")
    public List<ProductoAmazon> porAsin(@RequestParam List<String> asin) {
        return amazon.porAsin(asin);
    }
}
