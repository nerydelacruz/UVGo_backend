# Kits y artículos: versión básica

URL local con el perfil `dev`: `http://localhost:8070/uvgo/api`.
Los cuerpos de las peticiones usan `Content-Type: application/json`.

## Crear un kit base con artículos predeterminados

`POST /kits` devuelve `201` y el kit creado:

```json
{
  "name": "Kit de laboratorio",
  "description": "Materiales iniciales",
  "course": "Química",
  "price": 100.00,
  "active": true,
  "articulos": [
    {
      "nombre": "Bata",
      "descripcion": "Bata blanca, talla M",
      "cantidad": 1,
      "categoria": "Laboratorio",
      "observacionesCotizacion": "Confirmar disponibilidad"
    }
  ]
}
```

`articulos` es opcional; si se omite empieza vacío. Los kits anteriores a esta
migración también empiezan con una lista vacía: se pueden cargar sus artículos
predeterminados mediante el endpoint para agregar artículos. No se convierten
automáticamente las descripciones antiguas en artículos.

## Personalizar

`POST /kits/{kitId}/personalizar`, sin cuerpo, devuelve `201` con una copia nueva.
La copia tiene `kitBaseId` igual al ID del original, estado `PERSONALIZADO` y
artículos independientes con IDs nuevos. El original conserva estado `BASE`.
Solo se personalizan kits base; intentar copiar un kit personalizado devuelve `409`.
Usar el ID devuelto para modificar los artículos de la copia.

## Administrar artículos

| Método | Ruta | Resultado |
|---|---|---|
| POST | `/kits/{kitId}/articulos` | Agrega un artículo, `201`. |
| GET | `/kits/{kitId}/articulos` | Lista los artículos, `200`. |
| PUT | `/kits/{kitId}/articulos/{articuloId}` | Reemplaza los datos editables, `200`. |
| DELETE | `/kits/{kitId}/articulos/{articuloId}` | Retira físicamente el artículo, `204`. |
| GET | `/kits/{kitId}` | Consulta un kit con sus artículos, `200`. |

Para POST y PUT se usa el mismo objeto de artículo del ejemplo anterior.
Nombre, descripción y cantidad son obligatorios. Cantidad debe ser positiva,
con un máximo de nueve dígitos enteros y tres decimales. Categoría y observaciones
son opcionales. En PUT, omitir un campo opcional lo deja vacío.

El servidor asigna `id`, `kitId` y `fechaCreacion`; editar conserva la fecha original.
Los campos `kitBaseId` y `estado` del kit también los controla el servidor.
Un kit inexistente o un artículo que no pertenece al kit indicado devuelve `404`.
Los datos inválidos devuelven `400`.

## Alcance

- Cada artículo pertenece a un único kit; no hay catálogo global de artículos.
- Se pueden administrar artículos tanto del kit base como de una copia.
- `GET /kits` lista kits base y personalizados.
- El precio del kit se copia sin cambios. Los artículos no tienen precio y no se calcula una cotización.
- `active` conserva su significado de habilitado; `estado` distingue BASE/PERSONALIZADO.
- Esta versión no incluye usuarios, control de propiedad, historial ni flujo de aprobación.
- La migración Flyway V2 crea la tabla `articulos` y amplía `kits` al arrancar
  contra la base configurada. Las pruebas normales utilizan H2, no la base remota.
