# Recepción de PDF para materiales

Actividad del 30 de septiembre de 2026 del plan acordado en el chat «Evaluar IA para materiales».

## Operación

`POST /uvgo/api/extracciones/pdf`, con cuerpo `multipart/form-data` y un único archivo en el campo `archivo`.

En Swagger, abrir la operación de recepción, elegir **Try it out**, seleccionar un PDF y ejecutar. También se puede usar Postman: método POST, Body → form-data, clave `archivo` de tipo File. No establecer manualmente el encabezado multipart: el cliente debe generar su delimitador.

Respuesta HTTP 200:

```json
{
  "nombre": "materiales.pdf",
  "tamanoBytes": 1234,
  "estado": "RECIBIDO",
  "mensaje": "PDF recibido. La extracción de materiales todavía no está habilitada."
}
```

El tamaño del ejemplo es ilustrativo; el servidor devuelve el tamaño real.

## Validaciones

| Caso | HTTP | Resultado |
| --- | --- | --- |
| Falta el campo archivo | 400 | Solicita adjuntar el PDF |
| Archivo vacío | 400 | Informa que está vacío |
| Más de 5 MiB (5 242 880 bytes) | 413 | Informa que supera el límite |
| Extensión distinta de .pdf | 415 | Solicita un PDF |
| Tipo declarado incompatible | 415 | Solicita un PDF |
| Cabecera distinta de %PDF-1.x o %PDF-2.x | 415 | Informa que la cabecera no es válida |

Se acepta `application/pdf`, `application/octet-stream` o tipo ausente, siempre comprobando extensión y cabecera. Los errores devuelven un campo `mensaje`. La solicitud completa tiene un límite de 6 MiB para dejar espacio a los metadatos multipart. Estos límites se encuentran en `application.properties` y afectan las cargas multipart de toda la aplicación.

## Alcance de esta etapa

La comprobación de cabecera es preliminar: no demuestra que todas las páginas sean legibles ni detecta todos los PDF dañados o cifrados. Eso corresponde al lector de PDF de la siguiente actividad. Un PDF escaneado también puede superar esta validación; su falta de texto se detectará al intentar extraerlo.

El módulo no utiliza repositorios ni guarda documentos, kits o artículos. El contenedor puede usar almacenamiento temporal durante la solicitud. El archivo se descarta al terminar; cuando se implemente el análisis deberá procesarse dentro de la operación o definirse explícitamente una política de conservación. `RECIBIDO` no representa un trabajo pendiente ni tiene un identificador para consultas posteriores.

## Pruebas

`RecepcionPdfTests` cubre recepción, metadatos, nombre sin ruta, archivo ausente, vacío, tamaño excesivo, extensión, tipo y cabecera. Ejecutar `./gradlew.bat test` desde el backend con Java 21. Las pruebas de MockMvc verifican el límite del servicio; además, enviar un archivo de más de 5 MiB al servidor real para comprobar el rechazo del contenedor multipart y su mensaje.

La ejecución automatizada quedó pendiente en esta sesión porque Gradle no pudo acceder a su caché y no se autorizó ejecutar fuera del entorno restringido.
