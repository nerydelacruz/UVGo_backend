package com.UVGgo.backend.persistence.crud;

import com.UVGgo.backend.persistence.entity.ArticuloEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ArticuloCrudRepository extends CrudRepository<ArticuloEntity, Integer> {
    Optional<ArticuloEntity> findByNombreAndCategoria(String nombre, String categoria);
}
