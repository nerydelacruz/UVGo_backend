package com.UVGgo.backend.domain.repository;

import com.UVGgo.backend.domain.Kit;

import java.util.List;
import java.util.Optional;

public interface KitRepository {
    List<Kit> getAll();
    Kit agregar(Kit kit);
    Optional<Kit> getById(int kitId);
    Kit actualizar(Kit kit);
    void eliminar(int kitId);
    boolean tienePersonalizaciones(int kitId);
    List<Kit> getPersonalizados();
    List<Kit> getPersonalizadosPorUsuario(int usuarioId);
}
