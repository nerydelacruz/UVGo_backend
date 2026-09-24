package com.UVGgo.backend.domain.service;

import com.UVGgo.backend.domain.Kit;
import com.UVGgo.backend.domain.EstadoKit;
import com.UVGgo.backend.domain.Articulo;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import com.UVGgo.backend.domain.repository.KitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KitService {
    @Autowired
    private KitRepository kitRepository;

    public List<Kit> getAll() {
        return kitRepository.getAll();
    }

    public Kit agregar(Kit kit) {
        return kitRepository.agregar(kit);
    }

    public Kit consultar(int kitId) {
        return kitRepository.getById(kitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kit no encontrado"));
    }

    @Transactional
    public Kit personalizarKit(int kitId) {
        Kit base = consultar(kitId);
        if (base.getKitBaseId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seleccione un kit base para personalizar");
        }
        base.setKitBaseId(base.getKitId());
        base.setEstado(EstadoKit.PERSONALIZADO);
        return kitRepository.agregar(base);
    }

    public List<Articulo> listarArticulos(int kitId) {
        return consultar(kitId).getArticulos();
    }

    @Transactional
    public Articulo agregarArticulo(int kitId, Articulo datos) {
        Kit kit = consultar(kitId);
        kit.getArticulos().add(new Articulo(null, kitId, datos.nombre(), datos.descripcion(),
                datos.cantidad(), datos.categoria(), Instant.now(), datos.observacionesCotizacion()));
        List<Articulo> guardados = kitRepository.actualizar(kit).getArticulos();
        return guardados.getLast();
    }

    @Transactional
    public Articulo editarArticulo(int kitId, int articuloId, Articulo datos) {
        Kit kit = consultar(kitId);
        int posicion = posicionArticulo(kit, articuloId);
        Articulo anterior = kit.getArticulos().get(posicion);
        kit.getArticulos().set(posicion, new Articulo(articuloId, kitId, datos.nombre(), datos.descripcion(),
                datos.cantidad(), datos.categoria(), anterior.fechaCreacion(), datos.observacionesCotizacion()));
        return kitRepository.actualizar(kit).getArticulos().stream()
                .filter(articulo -> articulo.id().equals(articuloId)).findFirst().orElseThrow();
    }

    @Transactional
    public void retirarArticulo(int kitId, int articuloId) {
        Kit kit = consultar(kitId);
        kit.getArticulos().remove(posicionArticulo(kit, articuloId));
        kitRepository.actualizar(kit);
    }

    private int posicionArticulo(Kit kit, int articuloId) {
        for (int i = 0; i < kit.getArticulos().size(); i++) {
            if (kit.getArticulos().get(i).id().equals(articuloId)) return i;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Artículo no encontrado en este kit");
    }
}
