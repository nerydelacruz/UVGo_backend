package com.UVGgo.backend.domain.service;

import com.UVGgo.backend.domain.Kit;
import com.UVGgo.backend.domain.repository.KitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KitService {
    @Autowired
    private KitRepository kitRepository;

    public List<Kit> getAll() {
        return kitRepository.getAll();
    }
}
