package com.UVGgo.backend.persistence.mapper;

import com.UVGgo.backend.domain.Articulo;
import com.UVGgo.backend.persistence.entity.ArticuloEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ArticuloMapper {
    Articulo toArticulo(ArticuloEntity articuloEntity);
    List<Articulo> toArticulos(List<ArticuloEntity> articuloEntities);
    ArticuloEntity toArticuloEntity(Articulo articulo);
}
