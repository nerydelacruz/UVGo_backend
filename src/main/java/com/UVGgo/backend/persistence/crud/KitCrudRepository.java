package com.UVGgo.backend.persistence.crud;

import com.UVGgo.backend.domain.EstadoKit;
import com.UVGgo.backend.persistence.entity.KitEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface KitCrudRepository extends CrudRepository<KitEntity, Integer> {
    List<KitEntity> findAllByOrderByNombreAsc();
    boolean existsByKitBaseId(Integer kitBaseId);
    List<KitEntity> findByUsuarioIdOrderByNombreAsc(Integer usuarioId);
    List<KitEntity> findByEstadoPersonalizacionOrderByNombreAsc(EstadoKit estadoPersonalizacion);
}
