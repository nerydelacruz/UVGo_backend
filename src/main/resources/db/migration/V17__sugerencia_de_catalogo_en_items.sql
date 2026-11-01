-- Un material de texto libre puede parecerse a un artículo del catálogo sin estar seguros: se guarda como
-- sugerencia para que el administrador la vea al cotizar.
ALTER TABLE items_pedido ADD COLUMN sugerencia_articulo_id INTEGER;
ALTER TABLE items_pedido ADD COLUMN sugerencia_descripcion VARCHAR(300);
ALTER TABLE items_pedido ADD COLUMN sugerencia_precio NUMERIC(12,2);
ALTER TABLE items_pedido ADD COLUMN sugerencia_moneda VARCHAR(10);
