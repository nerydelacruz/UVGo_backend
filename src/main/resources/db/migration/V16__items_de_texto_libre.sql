-- Los materiales leídos de un PDF se solicitan como ítems de texto libre (solo descripción y cantidad).
-- El CHECK de V13 no tiene nombre estable entre PostgreSQL y H2; se recrea la columna para reemplazarlo.
ALTER TABLE items_pedido ADD COLUMN tipo_nuevo VARCHAR(20);
UPDATE items_pedido SET tipo_nuevo = tipo;
ALTER TABLE items_pedido DROP COLUMN tipo;
ALTER TABLE items_pedido RENAME COLUMN tipo_nuevo TO tipo;
ALTER TABLE items_pedido ADD CONSTRAINT items_pedido_tipo_check CHECK (tipo IN ('CATALOGO','WEB','TEXTO'));
