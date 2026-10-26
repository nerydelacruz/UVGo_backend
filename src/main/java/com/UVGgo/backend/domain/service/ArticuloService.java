package com.UVGgo.backend.domain.service;

import com.UVGgo.backend.domain.Articulo;
import com.UVGgo.backend.domain.ProductoBrightData;
import com.UVGgo.backend.domain.ResultadoImportacion;
import com.UVGgo.backend.domain.repository.ArticuloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ArticuloService {
    private static final int TAMANO_MAXIMO = 100;

    @Autowired
    private ArticuloRepository articuloRepository;

    public List<Articulo> listar(String q, int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "pagina debe ser >= 0 y tamano entre 1 y " + TAMANO_MAXIMO);
        }
        if (q == null || q.isBlank()) {
            return articuloRepository.getAll(pagina, tamano);
        }
        return articuloRepository.buscarPorNombre(q.trim(), pagina, tamano);
    }

    public Articulo consultar(int articuloId) {
        return articuloRepository.getById(articuloId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artículo no encontrado"));
    }

    @Transactional
    public Articulo agregar(Articulo datos) {
        validarIdExternoLibre(datos.idExterno(), null);
        // Una creación siempre genera un ID nuevo, aunque el cliente envíe uno.
        return articuloRepository.guardar(conId(null, datos));
    }

    @Transactional
    public Articulo editar(int articuloId, Articulo datos) {
        consultar(articuloId);
        validarIdExternoLibre(datos.idExterno(), articuloId);
        return articuloRepository.guardar(conId(articuloId, datos));
    }

    @Transactional
    public void eliminar(int articuloId) {
        consultar(articuloId);
        articuloRepository.eliminar(articuloId);
    }

    // Si el producto ya existe (mismo idExterno) se actualiza; si no, se crea.
    @Transactional
    public ResultadoImportacion importarBrightData(List<ProductoBrightData> productos) {
        int creados = 0, actualizados = 0, omitidos = 0;
        for (ProductoBrightData producto : productos) {
            if (producto == null || vacio(producto.titulo())) {
                omitidos++;
                continue;
            }
            // Sin identificador de la tienda, la URL del producto sirve para no duplicarlo.
            String idExterno = recortar(vacio(producto.idExterno()) ? producto.url() : producto.idExterno(), 255);
            Optional<Articulo> existente = idExterno == null ? Optional.empty() : articuloRepository.getByIdExterno(idExterno);
            articuloRepository.guardar(desdeBrightData(existente.map(Articulo::id).orElse(null), idExterno, producto));
            if (existente.isPresent()) actualizados++; else creados++;
        }
        return new ResultadoImportacion(creados, actualizados, omitidos);
    }

    private Articulo desdeBrightData(Integer id, String idExterno, ProductoBrightData p) {
        String nombre = recortar(p.titulo(), 300);
        // Algunas tiendas no traen descripción; se usa el título para cumplir con la columna obligatoria.
        String descripcion = vacio(p.descripcion()) ? nombre : recortar(p.descripcion(), 4000);
        Object precio = p.precio();
        String moneda = p.moneda();
        // Formato {value, currency, symbol}: la moneda del precio (GTQ) es más precisa que el símbolo suelto (Q).
        if (precio instanceof Map<?, ?> detalle) {
            precio = detalle.get("value");
            if (detalle.get("currency") != null) moneda = detalle.get("currency").toString();
        }
        String tienda = vacio(p.tienda()) ? dominio(p.url()) : p.tienda();
        return new Articulo(id, nombre, descripcion, recortar(categoria(p.categorias()), 100),
                recortar(p.imagen(), 2000), precio(precio == null ? null : precio.toString()), recortar(moneda, 10),
                recortar(tienda, 150), recortar(p.url(), 2000), idExterno);
    }

    private String dominio(String url) {
        if (vacio(url)) return null;
        try {
            String host = URI.create(url.trim()).getHost();
            return host == null ? null : host.replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // La categoría más específica suele ser la última de la lista.
    private String categoria(List<String> categorias) {
        if (categorias == null) return null;
        return categorias.reversed().stream().filter(c -> !vacio(c)).findFirst().orElse(null);
    }

    // Acepta números o textos como "$1,299.99"; si no se puede leer, el precio queda vacío.
    private BigDecimal precio(String texto) {
        if (vacio(texto)) return null;
        String limpio = texto.replaceAll("[^0-9.]", "");
        try {
            BigDecimal precio = new BigDecimal(limpio).setScale(2, RoundingMode.HALF_UP);
            return precio.precision() - precio.scale() > 10 ? null : precio;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void validarIdExternoLibre(String idExterno, Integer articuloId) {
        if (vacio(idExterno)) return;
        articuloRepository.getByIdExterno(idExterno)
                .filter(otro -> !otro.id().equals(articuloId))
                .ifPresent(otro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un artículo con ese idExterno");
                });
    }

    private Articulo conId(Integer id, Articulo a) {
        return new Articulo(id, a.nombre(), a.descripcion(), a.categoria(), a.imagen(), a.precio(),
                a.moneda(), a.tienda(), a.url(), vacio(a.idExterno()) ? null : a.idExterno());
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String recortar(String texto, int maximo) {
        if (vacio(texto)) return null;
        String limpio = texto.trim();
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }
}
