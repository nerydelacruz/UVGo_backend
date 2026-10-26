# Pedidos

Módulo para crear, listar, consultar, cancelar y dar seguimiento a pedidos. Cada
pedido guarda una lista fija de artículos del catálogo (`articulos`) con sus cantidades.

## Alcance

- `articuloId` es el ID del artículo en el catálogo (`GET /articulos`).
- Por ahora solo se valida que los IDs sean positivos. No se comprueba que el
  artículo exista ni se consulta o descuenta stock.
- No guarda nombres, precios, datos del cliente ni información de entrega. El ID por
  sí solo no permite reconstruir el contenido original si después cambia o se
  elimina el artículo.
- No incluye autenticación ni control de acceso. Listar o consultar no identifica
  al propietario del pedido. Las solicitudes con usuario están en `/api/v1/pedidos`
  (paquete `pedido`), que es otro módulo.

## Probar desde Postman

Con el perfil `dev`, la URL base es `http://localhost:8070/uvgo/api`. Las tablas
`pedidos` y `pedido_detalles` las crean las migraciones Flyway al arrancar.

### Crear

`POST /pedidos`, con `Content-Type: application/json`:

```json
{
  "detalles": [
    { "articuloId": 32, "cantidad": 2 },
    { "articuloId": 33, "cantidad": 1.5 }
  ]
}
```

Devuelve `201` con una estructura como esta (IDs y fechas los asigna el servidor):

```json
{
  "pedidoId": 1,
  "estado": "SOLICITADO",
  "fechaCreacion": "2026-09-28T18:00:00Z",
  "fechaCancelacion": null,
  "detalles": [
    { "id": 1, "articuloId": 32, "cantidad": 2 },
    { "id": 2, "articuloId": 33, "cantidad": 1.5 }
  ]
}
```

La lista debe contener al menos un detalle. Los IDs son enteros positivos y
`cantidad` admite de `0.001` a `999999999.999`, con hasta tres decimales.
No se permite repetir un `articuloId` dentro del mismo pedido. La creación se
guarda como una sola transacción. Cada POST válido crea un nuevo pedido; todavía no
hay prevención de duplicados por doble clic o reintentos.

### Consultar

- `GET /pedidos`: devuelve una lista con los detalles, ordenada por fecha de
  creación descendente; en caso de empate, por ID descendente. No tiene paginación.
- `GET /pedidos/1`: devuelve un pedido con sus detalles.

### Cancelar

`POST /pedidos/1/cancelar`, sin cuerpo, devuelve `200` con el pedido en estado
`CANCELADO` y su `fechaCancelacion`.

Solo pueden cancelarse pedidos en `SOLICITADO`. Sus detalles se conservan. Repetir la
cancelación devuelve el pedido ya cancelado sin modificar la fecha original.
La actualización comprueba el estado también en la base de datos para evitar
sobrescribir otro estado si dos operaciones coinciden.

### Seguimiento

Flujo de estados: `SOLICITADO → REVISION → COTIZADO → CONFIRMADO → EN_RUTA → ENTREGADO`.
`CANCELADO` es final y solo se alcanza con la ruta de cancelación.

- `GET /pedidos/1/estado`: devuelve `pedidoId`, `estado`, `siguienteEstado` y
  `anteriorEstado` (`null` cuando no hay).
- `PUT /pedidos/1/estado` con `{ "estado": "REVISION" }`: mueve el pedido un paso
  hacia adelante o hacia atrás (para corregir errores). Repetir el mismo estado
  devuelve el pedido sin cambios.

### Errores

| Código | Situación |
|---|---|
| `400` | Cuerpo inválido, IDs/cantidades inválidos, artículos repetidos o `CANCELADO` enviado a `PUT /estado`. |
| `404` | Pedido inexistente. |
| `409` | Cancelación fuera de `SOLICITADO`, salto de más de un estado o cambio concurrente. |
| `405` | Método no disponible, como DELETE sobre un pedido. |

## Pruebas automatizadas

```bash
./gradlew test --tests 'com.UVGgo.backend.pedidos.PedidoApiTests'
```

En Windows: `.\gradlew.bat test --tests 'com.UVGgo.backend.pedidos.PedidoApiTests'`.

Las pruebas usan H2 con el perfil `test` y las migraciones Flyway; no usan la base
remota del perfil `dev`.

## Estructura

Flujo: `PedidoController → PedidoService → PedidoRepository → PedidoEntityRepository
→ PedidoCrudRepository`.

Las clases están en subpaquetes `pedidos` dentro de las capas existentes. Los DTOs de
entrada son propios; las respuestas utilizan los records de dominio `Pedido`,
`PedidoDetalle` y `SeguimientoPedido`. MapStruct convierte las entidades a esos records.
