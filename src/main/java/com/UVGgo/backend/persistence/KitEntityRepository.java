package com.UVGgo.backend.persistence;

import com.UVGgo.backend.domain.Kit;
import com.UVGgo.backend.domain.repository.KitRepository;
import com.UVGgo.backend.persistence.crud.KitCrudRepository;
import com.UVGgo.backend.persistence.entity.KitEntity;
import com.UVGgo.backend.persistence.mapper.KitMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class KitEntityRepository implements KitRepository {
    @Autowired
    private KitCrudRepository kitCrudRepository;
    @Autowired
    private KitMapper mapper;

    @Override
    public List<Kit> getAll() {
        return mapper.toKits(kitCrudRepository.findAllByOrderByNombreAsc());
    }

    @Override
    public Kit agregar(Kit kit) {
        KitEntity entidad = mapper.toKitEntity(kit);
        // Una creación siempre genera un ID nuevo, aunque el cliente envíe uno.
        entidad.setIdKit(null);
        entidad.getArticulos().forEach(articulo -> {
            articulo.setId(null);
            articulo.setKit(entidad);
            articulo.setFechaCreacion(Instant.now());
        });
        return mapper.toKit(kitCrudRepository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Kit> getById(int kitId) {
        return kitCrudRepository.findById(kitId).map(mapper::toKit);
    }

    @Override
    public Kit actualizar(Kit kit) {
        KitEntity entidad = mapper.toKitEntity(kit);
        entidad.getArticulos().forEach(articulo -> articulo.setKit(entidad));
        return mapper.toKit(kitCrudRepository.save(entidad));
    }

    @Override
    public void eliminar(int kitId) {
        kitCrudRepository.deleteById(kitId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tienePersonalizaciones(int kitId) {
        return kitCrudRepository.existsByKitBaseId(kitId);
    }
}
