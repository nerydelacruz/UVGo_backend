-- Mientras los materiales de texto libre se buscan en el catálogo (en segundo plano) la solicitud queda marcada;
-- así el administrador no la abre a medias. La marca vence sola por si el proceso se interrumpe.
ALTER TABLE solicitudes ADD COLUMN busqueda_catalogo_desde TIMESTAMP WITH TIME ZONE;
