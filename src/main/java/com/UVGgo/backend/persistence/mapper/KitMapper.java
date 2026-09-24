package com.UVGgo.backend.persistence.mapper;

import com.UVGgo.backend.domain.Kit;
import com.UVGgo.backend.domain.Articulo;
import com.UVGgo.backend.persistence.entity.ArticuloEntity;
import com.UVGgo.backend.persistence.entity.KitEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface KitMapper {
    @Mappings({
            @Mapping(source = "idKit", target = "kitId"),
            @Mapping(source = "nombre", target = "name"),
            @Mapping(source = "descripcion", target = "description"),
            @Mapping(source = "curso", target = "course"),
            @Mapping(source = "precio", target = "price"),
            @Mapping(source = "estado", target = "active"),
            @Mapping(source = "estadoPersonalizacion", target = "estado")
    })
    Kit toKit(KitEntity kitEntity);
    List<Kit> toKits(List<KitEntity> kitEntities);

    @InheritInverseConfiguration
    KitEntity toKitEntity(Kit kit);

    @Mapping(source = "kit.idKit", target = "kitId")
    Articulo toArticulo(ArticuloEntity articulo);

    @Mapping(target = "kit", ignore = true)
    ArticuloEntity toArticuloEntity(Articulo articulo);
}
