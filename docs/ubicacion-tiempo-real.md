# Ubicación del repartidor en tiempo real

STOMP sobre WebSocket en `/uvgo/api/ws` (código en `ubicacion/` y `config/WebSocketConfig`, `config/AutenticacionStomp`).

| Qué | Destino | Quién |
|---|---|---|
| Publicar posición | `SEND /app/pedidos/{pedidoId}/ubicacion` | administrador (por ahora; el rol repartidor aún no existe) |
| Recibir posición | `SUBSCRIBE /topic/pedidos/{pedidoId}/ubicacion` | administrador |
| Última posición conocida | `GET /api/v1/pedidos/{pedidoId}/ubicacion` (204 si no hay) | administrador |

Cuerpo publicado: `{"latitud":14.6075,"longitud":-90.488,"rumbo":45}` (`rumbo` opcional, grados de brújula).
Lo que reciben los suscriptores añade `marca` (hora del servidor).

## Autenticación
El navegador no puede mandar `Authorization` al abrir un WebSocket, así que el JWT va en el header
`Authorization: Bearer ...` del frame **CONNECT** de STOMP. `AutenticacionStomp` lo valida con el mismo
`JwtDecoder` de la API (firma, vencimiento, sesión vigente, usuario activo). Sin token válido no hay conexión.
`SUBSCRIBE` y `SEND` exigen rol `ADMINISTRADOR` y solo aceptan los destinos de ubicación de un pedido.
La sesión se valida al conectar: si se revoca con el WebSocket ya abierto, sigue hasta que se cierre.

## Limitaciones
- La última posición vive en memoria (`UbicacionService`): se pierde al reiniciar y no se comparte entre instancias.
  Con varias instancias haría falta un broker externo (RabbitMQ/Redis) o persistir la posición.
- En Azure App Service hay que activar **Web sockets** (Configuración → General) o la conexión falla solo en producción.

## Simulador
En el repo del frontend: `scripts/simular-repartidor.mjs` (ver el encabezado del archivo).
