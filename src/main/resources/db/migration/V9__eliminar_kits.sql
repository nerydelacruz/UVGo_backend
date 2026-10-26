-- Se deja de usar todo lo relacionado con kits (acordado con el equipo).
DROP TABLE IF EXISTS kit_articulos;

-- articulos queda como catálogo independiente: se quitan las columnas propias del kit
-- (en la base compartida ya no existen; en una base nueva vienen de V2).
DROP INDEX IF EXISTS idx_articulos_kit;
ALTER TABLE articulos DROP COLUMN IF EXISTS kit_id;
ALTER TABLE articulos DROP COLUMN IF EXISTS cantidad;
ALTER TABLE articulos DROP COLUMN IF EXISTS fecha_creacion;
ALTER TABLE articulos DROP COLUMN IF EXISTS observaciones_cotizacion;

DROP TABLE IF EXISTS kits;
DROP TABLE IF EXISTS estados_kit;
