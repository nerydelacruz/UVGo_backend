package com.UVGgo.backend.persistence;

import com.UVGgo.backend.domain.Articulo;
import com.UVGgo.backend.domain.EstadoKit;
import com.UVGgo.backend.domain.Kit;
import com.UVGgo.backend.domain.repository.KitRepository;
import com.UVGgo.backend.persistence.crud.ArticuloCrudRepository;
import com.UVGgo.backend.persistence.crud.KitCrudRepository;
import com.UVGgo.backend.persistence.entity.ArticuloEntity;
import com.UVGgo.backend.persistence.entity.KitArticuloEntity;
import com.UVGgo.backend.persistence.entity.KitEntity;
import com.UVGgo.backend.persistence.mapper.KitMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
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
    private ArticuloCrudRepository articuloCrudRepository;
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
        entidad.setArticulos(construirArticulos(entidad, kit.getArticulos(), true));
        return mapper.toKit(kitCrudRepository.save(entidad));
    }

    // El artículo es un catálogo compartido: se reutiliza por nombre+categoría
    // en vez de duplicarlo cada vez que un kit lo usa.
    private ArticuloEntity resolverCatalogo(Articulo datos) {
        ArticuloEntity entidad = articuloCrudRepository.findByNombreAndCategoria(datos.nombre(), datos.categoria())
                .orElseGet(ArticuloEntity::new);
        entidad.setNombre(datos.nombre());
        entidad.setDescripcion(datos.descripcion());
        entidad.setCategoria(datos.categoria());
        return articuloCrudRepository.save(entidad);
    }

    private List<KitArticuloEntity> construirArticulos(KitEntity kitEntidad, List<Articulo> articulos, boolean nuevos) {
        List<KitArticuloEntity> lista = new ArrayList<>();
        for (Articulo datos : articulos) {
            KitArticuloEntity relacion = new KitArticuloEntity();
            relacion.setId(nuevos ? null : datos.id());
            relacion.setKit(kitEntidad);
            relacion.setArticulo(resolverCatalogo(datos));
            relacion.setCantidad(datos.cantidad());
            relacion.setObservacionesCotizacion(datos.observacionesCotizacion());
            relacion.setFechaCreacion(nuevos ? Instant.now() : datos.fechaCreacion());
            lista.add(relacion);
        }
        return lista;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Kit> getById(int kitId) {
        return kitCrudRepository.findById(kitId).map(mapper::toKit);
    }

    @Override
    public Kit actualizar(Kit kit) {
        KitEntity entidad = mapper.toKitEntity(kit);
        entidad.setArticulos(construirArticulos(entidad, kit.getArticulos(), false));
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

    @Override
    @Transactional(readOnly = true)
    public List<Kit> getPersonalizados() {
        return mapper.toKits(kitCrudRepository.findByEstadoPersonalizacionOrderByNombreAsc(EstadoKit.PERSONALIZADO));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Kit> getPersonalizadosPorUsuario(int usuarioId) {
        return mapper.toKits(kitCrudRepository.findByUsuarioIdOrderByNombreAsc(usuarioId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Kit> getBase() {
        return mapper.toKits(kitCrudRepository.findByEstadoPersonalizacionOrderByNombreAsc(EstadoKit.BASE));
    }
}
