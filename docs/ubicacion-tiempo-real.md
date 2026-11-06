# Pilotos y ubicación en tiempo real

Un administrador asigna un **piloto** (conductor) a cada pedido confirmado. El piloto es el simulador de Webtrack
(`Webtrack/simulator`): se entera de la asignación al instante y publica su posición, que el administrador ve moverse
en el mapa. Todo va por STOMP sobre WebSocket en `/uvgo/api/ws` (código en `piloto/`, `ubicacion/`,
`config/WebSocketConfig` y `config/AutenticacionStomp`).

```
Admin ──POST /pedidos/{id}/piloto──▶ Backend ──/topic/pilotos/{pilotoId}/asignaciones──▶ Simulador (piloto)
Simulador ──SEND /app/pedidos/{id}/ubicacion──▶ Backend ──/topic/pedidos/{id}/ubicacion──▶ Mapa del admin
```

## Asignar piloto (REST, solo administrador)
| Qué | Ruta |
|---|---|
| Pilotos disponibles | `GET /api/v1/pilotos` → `[{id, nombre}]` |
| Asignar o cambiar | `POST /api/v1/pedidos/{pedidoId}/piloto` con `{"version": n, "pilotoId": "uuid"}` |
| Quitar | `DELETE /api/v1/pedidos/{pedidoId}/piloto?version=n` |

Solo se asigna a un pedido confirmado que aún no se entrega (`CONFIRMADO`, `PAGADO`, `EN_COMPRA`, `LISTO`); si no, 409.
Usa la `version` del pedido como el resto de operaciones (409 si cambió). El detalle del pedido trae `piloto: {id, nombre}`
(o `null`). La migración `V20__pilotos.sql` crea la tabla `pilotos` y siembra **Piloto Oscar**
(`00000000-0000-4000-8000-000000000001`), el único por ahora; para agregar otro basta un `INSERT` en `pilotos`.

## WebSocket
| Qué | Destino | Quién |
|---|---|---|
| Mis pedidos vigentes (una vez, al suscribirse) | `SUBSCRIBE /app/pilotos/{pilotoId}/pedidos` | el propio piloto |
| Asignaciones nuevas o quitadas | `SUBSCRIBE /topic/pilotos/{pilotoId}/asignaciones` | el propio piloto |
| Publicar mi posición | `SEND /app/pedidos/{pedidoId}/ubicacion` | el piloto **asignado** a ese pedido |
| Ver la posición | `SUBSCRIBE /topic/pedidos/{pedidoId}/ubicacion` | administrador |
| Última posición conocida | `GET /api/v1/pedidos/{pedidoId}/ubicacion` (204 si no hay) | administrador |

Asignación que recibe el piloto: `{pedidoId, curso, destino, estado, asignado, marca}`; `asignado: false` avisa que
el pedido dejó de ser suyo. `destino` es el punto de entrega del estudiante (texto, puede ser `null`).
Posición que publica el piloto: `{"latitud":14.6075,"longitud":-90.488,"rumbo":45}` (`rumbo` opcional, grados de
brújula). Los suscriptores la reciben con `marca` (hora del servidor).

## Autenticación (frame CONNECT de STOMP)
El navegador no puede mandar `Authorization` al abrir un WebSocket, así que las credenciales van en el CONNECT:
- **Administrador**: `Authorization: Bearer <jwt>`. Se valida con el mismo `JwtDecoder` de la API (firma, vencimiento,
  sesión vigente, usuario activo).
- **Piloto / simulador**: `X-Piloto-Id: <uuid>` y `X-Piloto-Clave: <SIMULADOR_CLAVE>`. La clave es una variable de
  entorno del servidor (mínimo 16 caracteres, generar con `openssl rand -base64 24`); sin ella el acceso de pilotos
  queda deshabilitado. Es una clave compartida: quien la tenga puede conectarse como cualquier piloto activo.

Cada rol solo puede usar sus destinos (`AutenticacionStomp`): un piloto no escucha pedidos ajenos ni la ubicación,
y no publica en pedidos que no tiene asignados (se consulta la base en cada posición, así que si le quitan el pedido
deja de poder publicar). Un administrador no publica posiciones. La sesión del administrador se valida solo al
conectar: si se revoca con el WebSocket ya abierto, sigue hasta que se cierre.

## Configuración
- `SIMULADOR_CLAVE` en `.env` (ver `.env.example`).
- `CORS_ORIGINS` también rige el origen del WebSocket: agregar el del simulador, p. ej.
  `CORS_ORIGINS=http://localhost:5173,http://localhost:3000`.
- En Azure App Service hay que activar **Web sockets** (Configuración → General) o la conexión falla solo en producción.

## Limitaciones
- La última posición vive en memoria (`UbicacionService`): se pierde al reiniciar y no se comparte entre instancias.
  Con varias instancias haría falta un broker externo (RabbitMQ/Redis) o persistir la posición.
- Los pilotos no tienen cuenta propia (no inician sesión como usuarios): solo existen en `pilotos` y entran con la clave
  compartida. Si más adelante necesitan login propio, habría que ampliar `usuarios` con un tipo `PILOTO`.
- No existe un estado "en ruta" en `EstadoPedido`: el pedido sigue `CONFIRMADO`/`PAGADO` mientras el piloto lo entrega.

## Simulador
`Webtrack/simulator`: botón **UVGo** (arriba a la derecha). Se conecta con la URL de la API, el piloto y la clave,
lista los pedidos asignados, arma la ruta bodega → destino y, al simular, envía cada posición también a UVGo.
