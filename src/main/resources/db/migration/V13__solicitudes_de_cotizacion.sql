-- Las solicitudes pasan a ser solicitudes de cotización: el estudiante elige productos del catálogo
-- (articulos) o de la web, el administrador les pone precio (COTIZADO) y el estudiante acepta (CONFIRMADO).
-- Cada ítem guarda una copia del producto para que la cotización no cambie si el artículo se edita o elimina.
ALTER TABLE items_pedido ALTER COLUMN unidad_medida DROP NOT NULL;
ALTER TABLE items_pedido ADD COLUMN tipo VARCHAR(20) CHECK (tipo IN ('CATALOGO','WEB'));
ALTER TABLE items_pedido ADD COLUMN articulo_id INTEGER;
ALTER TABLE items_pedido ADD COLUMN url VARCHAR(2000);
ALTER TABLE items_pedido ADD COLUMN imagen VARCHAR(2000);
ALTER TABLE items_pedido ADD COLUMN tienda VARCHAR(150);
ALTER TABLE items_pedido ADD COLUMN precio_referencia NUMERIC(12,2);
ALTER TABLE items_pedido ADD COLUMN moneda_referencia VARCHAR(10);
ALTER TABLE items_pedido ADD COLUMN precio_unitario NUMERIC(12,2) CHECK (precio_unitario >= 0);

ALTER TABLE solicitudes ADD COLUMN cargo_servicio NUMERIC(12,2) CHECK (cargo_servicio >= 0);
ALTER TABLE solicitudes ADD COLUMN notas_cotizacion VARCHAR(2000);
ALTER TABLE solicitudes ADD COLUMN fecha_cotizacion TIMESTAMP WITH TIME ZONE;
ALTER TABLE solicitudes ADD COLUMN fecha_confirmacion TIMESTAMP WITH TIME ZONE;
