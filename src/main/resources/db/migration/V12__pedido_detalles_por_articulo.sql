-- En la base compartida, V8 (que no está en este repositorio) ya cambió pedido_detalles para
-- referirse a artículos en vez de kits. Aquí se repite ese cambio para bases nuevas (pruebas);
-- en la compartida cada sentencia no hace nada porque la tabla ya tiene esta forma.
ALTER TABLE pedido_detalles DROP CONSTRAINT IF EXISTS uq_pedido_detalle_referencia;
ALTER TABLE pedido_detalles DROP COLUMN IF EXISTS kit_id;
ALTER TABLE pedido_detalles DROP COLUMN IF EXISTS kit_articulo_id;
ALTER TABLE pedido_detalles ADD COLUMN IF NOT EXISTS articulo_id INTEGER NOT NULL CHECK (articulo_id > 0);
CREATE UNIQUE INDEX IF NOT EXISTS uq_pedido_detalle_articulo ON pedido_detalles(pedido_id, articulo_id);
