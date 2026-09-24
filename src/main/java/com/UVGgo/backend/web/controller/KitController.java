package com.UVGgo.backend.web.controller;

import com.UVGgo.backend.domain.Kit;
import com.UVGgo.backend.domain.service.KitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/kits")
public class KitController {
    @Autowired
    private KitService kitService;

    @GetMapping
    public List<Kit> getAll() {
        return kitService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Kit agregar(@Valid @RequestBody Kit kit) {
        return kitService.agregar(kit);
    }

    @GetMapping("/{kitId}")
    public Kit consultar(@PathVariable int kitId) {
        return kitService.consultar(kitId);
    }

    @PostMapping("/{kitId}/personalizar")
    @ResponseStatus(HttpStatus.CREATED)
    public Kit personalizarKit(@PathVariable int kitId) {
        return kitService.personalizarKit(kitId);
    }
}
