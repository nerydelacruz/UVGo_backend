package com.UVGgo.backend.domain.repository;

import com.UVGgo.backend.domain.Articulo;

import java.util.List;
import java.util.Optional;

public interface ArticuloRepository {
    List<Articulo> getAll(int pagina, int tamano);
    List<Articulo> buscarPorNombre(String nombre, int pagina, int tamano);
    Optional<Articulo> getById(int articuloId);
    Optional<Articulo> getByIdExterno(String idExterno);
    Articulo guardar(Articulo articulo);
    void eliminar(int articuloId);
}
