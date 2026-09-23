package com.UVGgo.backend.web.controller;

import com.UVGgo.backend.domain.Kit;
import com.UVGgo.backend.domain.service.KitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
