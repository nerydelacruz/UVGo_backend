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
        // Un kit creado directamente siempre es base; las copias salen de personalizarKit.
        kit.setUsuarioId(null);
        kit.setKitBaseId(null);
        kit.setEstado(EstadoKit.BASE);
        return kitRepository.agregar(kit);
    }

    public Kit consultar(int kitId) {
        return kitRepository.getById(kitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kit no encontrado"));
    }

    @Transactional
    public Kit editar(int kitId, Kit datos) {
        Kit kit = consultar(kitId);
        kit.setName(datos.getName());
        kit.setDescription(datos.getDescription());
        kit.setCourse(datos.getCourse());
        kit.setPrice(datos.getPrice());
        kit.setActive(datos.isActive());
        return kitRepository.actualizar(kit);
    }

    @Transactional
    public void eliminar(int kitId) {
        consultar(kitId);
        if (kitRepository.tienePersonalizaciones(kitId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar un kit base con copias personalizadas");
        }
        kitRepository.eliminar(kitId);
    }

    @Transactional
    public Kit personalizarKit(Kit datos) {
        if (datos.getKitId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique el kitId del kit base a personalizar");
        }
        Kit base = consultar(datos.getKitId());
        if (base.getKitBaseId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seleccione un kit base para personalizar");
        }
        // El kit personalizado se arma con los datos y la lista final de artículos que envía el usuario.
        Kit personalizado = new Kit();
        personalizado.setName(datos.getName());
        personalizado.setDescription(datos.getDescription());
        personalizado.setCourse(datos.getCourse());
        personalizado.setPrice(datos.getPrice());
        personalizado.setActive(true);
        personalizado.setArticulos(datos.getArticulos());
        personalizado.setKitBaseId(base.getKitId());
        personalizado.setEstado(EstadoKit.PERSONALIZADO);
        personalizado.setUsuarioId(datos.getUsuarioId());
        return kitRepository.agregar(personalizado);
    }

    // Mientras no exista la opción de usuario, sin usuarioId se listan todos los kits personalizados.
    public List<Kit> listarPersonalizados(Integer usuarioId) {
        if (usuarioId == null) {
            return kitRepository.getPersonalizados();
        }
        return kitRepository.getPersonalizadosPorUsuario(usuarioId);
    }

    public List<Articulo> listarArticulos(int kitId) {
        return consultar(kitId).getArticulos();
    }

    public Articulo consultarArticulo(int kitId, int articuloId) {
        Kit kit = consultar(kitId);
        return kit.getArticulos().get(posicionArticulo(kit, articuloId));
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
