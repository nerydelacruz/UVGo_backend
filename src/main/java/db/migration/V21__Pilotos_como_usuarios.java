package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Los pilotos pasan a ser el tercer tipo de usuario (inician sesión como cualquier otro).
 *
 * 1. usuarios.tipo admite 'PILOTO'. El CHECK de V11 no tiene nombre estable entre PostgreSQL y H2, por eso
 *    se busca por su contenido y se reemplaza por uno con nombre (no se toca ningún dato).
 * 2. solicitudes.piloto_usuario_id apunta al usuario piloto asignado.
 *
 * No borra nada: la tabla 'pilotos' y la columna solicitudes.piloto_id de V20 (el piloto del simulador)
 * quedan sin uso; eliminarlas es un cambio destructivo que se decide aparte.
 */
public class V21__Pilotos_como_usuarios extends BaseJavaMigration {
    @Override public void migrate(Context context) throws Exception {
        Connection conexion=context.getConnection();
        String esquema=conexion.getSchema();
        List<String> checksDeTipo=new ArrayList<>();
        String consulta="SELECT tc.constraint_name, tc.table_schema, cc.check_clause "
                +"FROM information_schema.table_constraints tc "
                +"JOIN information_schema.check_constraints cc "
                +"ON cc.constraint_schema=tc.constraint_schema AND cc.constraint_name=tc.constraint_name "
                +"WHERE tc.constraint_type='CHECK' AND LOWER(tc.table_name)='usuarios'";
        try (PreparedStatement ps=conexion.prepareStatement(consulta); ResultSet rs=ps.executeQuery()) {
            while (rs.next()) {
                String clausula=rs.getString(3);
                boolean delEsquema=esquema==null || esquema.equalsIgnoreCase(rs.getString(2));
                // El CHECK de tipos es el único que nombra a los dos tipos originales
                if (delEsquema && clausula!=null && clausula.contains("ESTUDIANTE") && clausula.contains("ADMINISTRADOR"))
                    checksDeTipo.add(rs.getString(1));
            }
        }
        if (checksDeTipo.isEmpty())
            throw new IllegalStateException("No se encontró el CHECK de usuarios.tipo creado en V11; revise el esquema antes de migrar");
        try (Statement st=conexion.createStatement()) {
            for (String nombre:checksDeTipo)
                st.execute("ALTER TABLE usuarios DROP CONSTRAINT \""+nombre.replace("\"","\"\"")+"\"");
            st.execute("ALTER TABLE usuarios ADD CONSTRAINT usuarios_tipo_check CHECK (tipo IN ('ESTUDIANTE','ADMINISTRADOR','PILOTO'))");
            st.execute("ALTER TABLE solicitudes ADD COLUMN piloto_usuario_id UUID REFERENCES usuarios(id)");
            st.execute("CREATE INDEX idx_solicitudes_piloto_usuario ON solicitudes(piloto_usuario_id)");
        }
    }
}
