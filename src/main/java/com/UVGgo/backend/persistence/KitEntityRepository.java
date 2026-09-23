package com.UVGgo.backend.persistence;

import com.UVGgo.backend.domain.Kit;
import com.UVGgo.backend.domain.repository.KitRepository;
import com.UVGgo.backend.persistence.crud.KitCrudRepository;
import com.UVGgo.backend.persistence.mapper.KitMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class KitEntityRepository implements KitRepository {
    @Autowired
    private KitCrudRepository kitCrudRepository;
    @Autowired
    private KitMapper mapper;

    @Override
    public List<Kit> getAll() {
        return mapper.toKits(kitCrudRepository.findAllByOrderByNombreAsc());
    }
}
