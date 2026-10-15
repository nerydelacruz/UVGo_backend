package com.UVGgo.backend.persistence.crud;

import com.UVGgo.backend.persistence.entity.ArticuloEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ArticuloCrudRepository extends CrudRepository<ArticuloEntity, Integer> {
    List<ArticuloEntity> findAllByOrderByNombreAsc(Pageable pagina);
    List<ArticuloEntity> findByNombreContainingIgnoreCaseOrderByNombreAsc(String nombre, Pageable pagina);
    Optional<ArticuloEntity> findByIdExterno(String idExterno);
}
