# Extracción de productos desde PDF con Claude

El estudiante sube un PDF (por ejemplo, la lista de materiales de un curso) y recibe los productos que pide con su cantidad. Se revisan en el frontend y se agregan a la solicitud. No se guarda nada en la base de datos.

## Flujo

```
PDF ──► PDFBox saca el texto ──► Claude (texto + instrucción corta) ──► { productos: [{ nombre, cantidad }] }
```

- `POST /uvgo/api/api/v1/extracciones/productos`, multipart, campo `archivo`. Requiere sesión de estudiante.
- `ExtraccionService` lee el PDF con PDFBox y llama a Claude con el SDK oficial (`com.anthropic:anthropic-java`).
- La respuesta se tipa con el record `ProductosExtraidos`; el SDK genera el JSON schema.

## Por qué así (ahorro de tokens)

- Se manda solo el texto del PDF, no el PDF: Claude cobra cada página como texto + imagen.
- Se pide solo `nombre` y `cantidad`: los tokens de salida son los más caros.
- Se compactan espacios y saltos de línea, y el texto se limita a 40,000 caracteres (~10,000 tokens).
- Los PDF escaneados (sin texto) se rechazan en lugar de mandarse como imagen.

## Configuración

En `.env`:

| Variable | Valor |
| --- | --- |
| `ANTHROPIC_API_KEY` | Clave de Claude Console. Nunca en el frontend. |
| `ANTHROPIC_MODEL` | Opcional. Por defecto `claude-opus-5-5`; `claude-haiku-5-5` es el más barato. |

Sin clave, el endpoint responde 503 y el resto del sistema funciona igual.
