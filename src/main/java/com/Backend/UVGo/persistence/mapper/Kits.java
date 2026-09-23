package com.Backend.UVGo.persistence.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import java.util.List;

//@Mapper(componentModel = "spring"), uses = {CategoryMapper.class}
public interface Kits {
//    @Mappings({
//            @Mapping(source = "", target = ""),
//    })
//   Kits toKits(Kits kits);
//    List<Product> toProducts(List<Producto>productos)

      @InheritInverseConfiguration
//    @Mapping(target = "codigoBarras", ignore = true)
//    Producto toProducto(Product product);


}
