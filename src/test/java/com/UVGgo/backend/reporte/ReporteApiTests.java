package com.UVGgo.backend.reporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.UVGgo.backend.pedido.Pedido;
import com.UVGgo.backend.pedido.PedidoDtos;
import com.UVGgo.backend.pedido.PedidoRepository;
import com.UVGgo.backend.piloto.Piloto;
import com.UVGgo.backend.usuario.Administrador;
import com.UVGgo.backend.usuario.Estudiante;
import com.UVGgo.backend.usuario.UsuarioRepository;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReporteApiTests {
    static final String PASSWORD="Prueba-local-123!";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired PedidoRepository pedidos;
    @Autowired PasswordEncoder passwords;

    private String correo() { return "reporte."+UUID.randomUUID()+"@uvg.edu.gt"; }
    private String login(String correo) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\""+correo+"\",\"contrasena\":\""+PASSWORD+"\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }
    private String loginAdmin() throws Exception {
        String c=correo();
        usuarios.saveAndFlush(new Administrador("Administrador ficticio",c,"55550000",passwords.encode(PASSWORD),"PRUEBA"));
        return login(c);
    }
    private Estudiante estudiante() {
        return usuarios.saveAndFlush(new Estudiante("Estudiante ficticio",correo(),"55550000",passwords.encode(PASSWORD),
                Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits()),"Carrera de prueba"));
    }
    private String hoy() { return LocalDate.now(ReporteService.ZONA).toString(); }

    /** Pedido confirmado con un producto web de una tienda única: 3 × Q10.00 + Q5.00 de envío. */
    private Pedido pedidoDe(String tienda,Piloto piloto) {
        var web=new PedidoDtos.ProductoWeb("Multímetro digital","https://example.com/multimetro",null,tienda,new BigDecimal("9.50"),"GTQ");
        var solicitud=new PedidoDtos.Solicitud("Física 2","Docente ficticio",LocalDate.now().plusDays(10),null,"Edificio A",
                List.of(new PedidoDtos.Producto(null,web,null,null,null,3)));
        Pedido p=pedidos.saveAndFlush(new Pedido(estudiante(),solicitud,Map.of()));
        Map<UUID,BigDecimal> precios=new HashMap<>();
        p.getItems().forEach(i -> precios.put(i.getId(),new BigDecimal("10.00")));
        p.cotizar(precios,new BigDecimal("5.00"),null);
        p.aceptar();
        if (piloto!=null) p.asignarPiloto(piloto);
        return pedidos.saveAndFlush(p);
    }

    @Test void elReporteSumaVentasEnvioProveedoresYKilometrajeDelPiloto() throws Exception {
        String tienda="Tienda "+UUID.randomUUID(), nombrePiloto="Piloto "+UUID.randomUUID();
        Piloto piloto=usuarios.saveAndFlush(new Piloto(nombrePiloto,correo(),"55550000",passwords.encode(PASSWORD)));
        Pedido p=pedidoDe(tienda,piloto);
        pedidos.registrarTramo(p.getId(),new BigDecimal("3.250"),600,new BigDecimal("24.50"));
        pedidos.registrarTramo(p.getId(),new BigDecimal("1.000"),300,new BigDecimal("31.00"));

        mvc.perform(get("/api/v1/reportes").param("desde",hoy()).param("hasta",hoy()).header("Authorization","Bearer "+loginAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("desde").value(hoy()))
                .andExpect(jsonPath("porDia.length()").value(1))
                .andExpect(jsonPath("resumen.pedidos").isNumber())
                .andExpect(jsonPath("porEstado.CONFIRMADO").isNumber())
                .andExpect(jsonPath("proveedores[?(@.tienda=='"+tienda+"')].monto").value(30.00))
                .andExpect(jsonPath("proveedores[?(@.tienda=='"+tienda+"')].unidades").value(3.0))
                .andExpect(jsonPath("proveedores[?(@.tienda=='"+tienda+"')].pedidos").value(1))
                .andExpect(jsonPath("pilotos[?(@.nombre=='"+nombrePiloto+"')].pedidos").value(1))
                .andExpect(jsonPath("pilotos[?(@.nombre=='"+nombrePiloto+"')].enCurso").value(1))
                .andExpect(jsonPath("pilotos[?(@.nombre=='"+nombrePiloto+"')].kilometros").value(4.25))
                .andExpect(jsonPath("pilotos[?(@.nombre=='"+nombrePiloto+"')].cargosEnvio").value(5.00))
                // 4.25 km en 900 s = 17 km/h; la máxima es la mayor de las registradas
                .andExpect(jsonPath("pilotos[?(@.nombre=='"+nombrePiloto+"')].velocidadPromedio").value(17.0))
                .andExpect(jsonPath("pilotos[?(@.nombre=='"+nombrePiloto+"')].velocidadMaxima").value(31.0));
    }
    @Test void elKilometrajeNoCambiaLaVersionDelPedido() throws Exception {
        Pedido p=pedidoDe("Tienda "+UUID.randomUUID(),null);
        long version=p.getVersion();
        pedidos.registrarTramo(p.getId(),new BigDecimal("2.000"),240,new BigDecimal("30.00"));
        Pedido recargado=pedidos.findById(p.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(recargado.getVersion()).isEqualTo(version);
        org.assertj.core.api.Assertions.assertThat(recargado.getKilometros()).isEqualByComparingTo("2.000");
        org.assertj.core.api.Assertions.assertThat(recargado.getSegundosEnRuta()).isEqualTo(240);
        org.assertj.core.api.Assertions.assertThat(recargado.getVelocidadMaxima()).isEqualByComparingTo("30.00");
    }
    @Test void sinFechasDevuelveLosUltimos30Dias() throws Exception {
        mvc.perform(get("/api/v1/reportes").header("Authorization","Bearer "+loginAdmin()))
                .andExpect(status().isOk()).andExpect(jsonPath("hasta").value(hoy())).andExpect(jsonPath("porDia.length()").value(30));
    }
    @Test void rechazaUnPeriodoInvertidoODemasiadoLargo() throws Exception {
        String token=loginAdmin();
        mvc.perform(get("/api/v1/reportes").param("desde","2026-05-10").param("hasta","2026-05-01").header("Authorization","Bearer "+token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/reportes").param("desde","2024-01-01").param("hasta","2026-01-01").header("Authorization","Bearer "+token))
                .andExpect(status().isBadRequest());
    }
    @Test void soloUnAdministradorVeLosReportes() throws Exception {
        mvc.perform(get("/api/v1/reportes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/reportes").header("Authorization","Bearer "+login(estudiante().getCorreo())))
                .andExpect(status().isForbidden());
    }
}
