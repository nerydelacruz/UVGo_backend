package com.UVGgo.backend.persistence;

import com.UVGgo.backend.domain.Articulo;
import com.UVGgo.backend.domain.repository.ArticuloRepository;
import com.UVGgo.backend.persistence.crud.ArticuloCrudRepository;
import com.UVGgo.backend.persistence.mapper.ArticuloMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class ArticuloEntityRepository implements ArticuloRepository {
    @Autowired
    private ArticuloCrudRepository articuloCrudRepository;
    @Autowired
    private ArticuloMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<Articulo> getAll(int pagina, int tamano) {
        return mapper.toArticulos(articuloCrudRepository.findAllByOrderByNombreAsc(PageRequest.of(pagina, tamano)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Articulo> buscarPorNombre(String nombre, int pagina, int tamano) {
        return mapper.toArticulos(articuloCrudRepository
                .findByNombreContainingIgnoreCaseOrderByNombreAsc(nombre, PageRequest.of(pagina, tamano)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Articulo> getById(int articuloId) {
        return articuloCrudRepository.findById(articuloId).map(mapper::toArticulo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Articulo> getByIdExterno(String idExterno) {
        return articuloCrudRepository.findByIdExterno(idExterno).map(mapper::toArticulo);
    }

    @Override
    public Articulo guardar(Articulo articulo) {
        return mapper.toArticulo(articuloCrudRepository.save(mapper.toArticuloEntity(articulo)));
    }

    @Override
    public void eliminar(int articuloId) {
        articuloCrudRepository.deleteById(articuloId);
    }
}
