-- Los títulos y descripciones de tiendas en línea suelen ser más largos.
ALTER TABLE articulos ALTER COLUMN nombre SET DATA TYPE VARCHAR(300);
ALTER TABLE articulos ALTER COLUMN descripcion SET DATA TYPE VARCHAR(4000);

ALTER TABLE articulos ADD COLUMN imagen VARCHAR(2000);
ALTER TABLE articulos ADD COLUMN precio NUMERIC(12,2);
ALTER TABLE articulos ADD COLUMN moneda VARCHAR(10);
ALTER TABLE articulos ADD COLUMN tienda VARCHAR(150);
ALTER TABLE articulos ADD COLUMN url VARCHAR(2000);
-- Identificador del producto en la tienda (ASIN, SKU...): evita duplicados al reimportar.
ALTER TABLE articulos ADD COLUMN id_externo VARCHAR(255);
CREATE UNIQUE INDEX uq_articulos_id_externo ON articulos(id_externo);
