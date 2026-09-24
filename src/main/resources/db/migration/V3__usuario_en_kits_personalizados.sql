ALTER TABLE kits ADD COLUMN usuario_id INTEGER;
CREATE INDEX idx_kits_usuario ON kits(usuario_id);
