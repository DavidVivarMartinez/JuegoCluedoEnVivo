package com.misterioenvivo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.misterioenvivo.servicio.MecanicasService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mecánicas de juego: cuaderno, tablón, QR, habilidades, tratos, pistas falsas, envíos y ranking. */
@SpringBootTest
@AutoConfigureMockMvc
class MecanicasTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    MecanicasService mecanicas;

    @Test
    void cuadernoDeDeduccionYAcusacionFinal() throws Exception {
        String master = crearPartida();
        long ana = crearJugador(master, "Ana", "Una llave inglesa");
        long luis = crearJugador(master, "Luis", "Un teclado");
        JsonNode estado = leer(get("/api/master/estado"), master);
        String codAna = codigoDe(estado, ana);
        String codLuis = codigoDe(estado, luis);
        sinContenido(put("/api/master/reglas"), master, "{\"lugares\":\"Despacho\\n Cocina \\n\\nJardín\",\"precioPistaFalsa\":200}");

        JsonNode panel = leer(get("/api/yo"), codAna);
        assertThat(panel.get("lugares")).extracting(JsonNode::asText).containsExactly("Despacho", "Cocina", "Jardín");

        hacer(put("/api/yo/deduccion"), codAna, "{\"categoria\":\"sospechoso\",\"clave\":\"" + luis + "\",\"marca\":\"si\"}", status().isOk());
        hacer(put("/api/yo/deduccion"), codAna, "{\"categoria\":\"LUGAR\",\"clave\":\"Cocina\",\"marca\":\"NO\"}", status().isOk());
        hacer(put("/api/yo/deduccion"), codAna, "{\"categoria\":\"LUGAR\",\"clave\":\"Ático\",\"marca\":\"NO\"}", status().isBadRequest());
        hacer(put("/api/yo/deduccion"), codAna, "{\"categoria\":\"ARMA\",\"clave\":\"" + ana + "\",\"marca\":\"QUIZA\"}", status().isBadRequest());
        assertThat(leer(get("/api/yo"), codAna).get("deduccion")).hasSize(2);
        // Borrar una casilla
        hacer(put("/api/yo/deduccion"), codAna, "{\"categoria\":\"LUGAR\",\"clave\":\"Cocina\",\"marca\":null}", status().isOk());
        assertThat(leer(get("/api/yo"), codAna).get("deduccion")).hasSize(1);
        // Es privado: Luis no ve nada
        assertThat(leer(get("/api/yo"), codLuis).get("deduccion")).isEmpty();

        // Acusación final: el Máster la ve en el ranking; el grupo, solo al final
        sinContenido(put("/api/master/jugadores/" + luis + "/asesino"), master, "{\"asesino\":true}");
        sinContenido(put("/api/yo/arma"), codLuis, "{\"jugadorId\":" + ana + "}");
        hacer(put("/api/yo/acusacion-final"), codAna,
                "{\"sospechosoId\":" + luis + ",\"armaDeId\":" + ana + ",\"razon\":\"El barro\"}", status().isOk());
        assertThat(leer(get("/api/yo"), codLuis).get("acusacionesFinales")).isEmpty();
        JsonNode filaAna = filaRanking(leer(get("/api/master/estado"), master), ana);
        assertThat(filaAna.get("aciertaAsesino").asBoolean()).isTrue();
        assertThat(filaAna.get("aciertaArma").asBoolean()).isTrue();
        assertThat(filaAna.get("puntos").asInt()).isEqualTo(150);
        assertThat(filaRanking(leer(get("/api/master/estado"), master), luis).get("puntos").asInt()).isZero();

        sinContenido(put("/api/master/fase"), master, "{\"fase\":\"FINALIZADA\"}");
        assertThat(leer(get("/api/yo"), codLuis).get("acusacionesFinales")).hasSize(1);
        hacer(put("/api/yo/acusacion-final"), codAna, "{\"sospechosoId\":" + ana + "}", status().isConflict());
    }

    @Test
    void validarDosVecesNoPagaDosVeces() throws Exception {
        String master = crearPartida();
        long ana = crearJugador(master, "Ana", null);
        String codAna = codigoDe(leer(get("/api/master/estado"), master), ana);
        long objetivo = crear(post("/api/master/jugadores/" + ana + "/objetivos"), master,
                "{\"texto\":\"Ganar una discusión\",\"recompensaDinero\":25}");
        sinContenido(put("/api/master/objetivos/" + objetivo), master, "{\"estado\":\"CUMPLIDO\"}");
        sinContenido(put("/api/master/objetivos/" + objetivo), master, "{\"estado\":\"ACTIVO\"}");
        sinContenido(put("/api/master/objetivos/" + objetivo), master, "{\"estado\":\"CUMPLIDO\"}");
        assertThat(leer(get("/api/yo"), codAna).get("dinero").asInt()).isEqualTo(25);
    }

    @Test
    void tablonPublicoQrYHabilidades() throws Exception {
        String master = crearPartida();
        long ana = crearJugador(master, "Ana", null);
        long luis = crearJugador(master, "Luis", null);
        JsonNode estado = leer(get("/api/master/estado"), master);
        String codAna = codigoDe(estado, ana);
        String codLuis = codigoDe(estado, luis);
        long pista = crear(post("/api/master/pistas"), master, "{\"titulo\":\"Coartada\",\"contenido\":\"Estaba en la terraza\"}");
        sinContenido(put("/api/master/pistas/" + pista + "/visibilidad"), master, "{\"jugadorIds\":[" + luis + "]}");

        // Acusación pública y respuesta con pista: todos lo ven
        long acusacion = crear(post("/api/yo/tablon"), codAna, "{\"acusadoId\":" + luis + ",\"texto\":\"Mientes sobre la terraza\"}");
        hacer(post("/api/yo/tablon/" + acusacion + "/respuesta"), codAna, "{\"texto\":\"No soy yo\"}", status().isNotFound());
        sinContenido(post("/api/yo/tablon/" + acusacion + "/respuesta"), codLuis, "{\"texto\":\"Mira esto\",\"pistaId\":" + pista + "}");
        JsonNode entrada = leer(get("/api/yo"), codAna).get("tablon").get(0);
        assertThat(entrada.get("respuesta").asText()).isEqualTo("Mira esto");
        assertThat(entrada.get("pistaMostrada").get("contenido").asText()).isEqualTo("Estaba en la terraza");

        // QR: quien teclea el código se queda con la pista
        String qr = json.readTree(mvc.perform(put("/api/master/pistas/" + pista + "/qr").header("Authorization", "Bearer " + master)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("codigo").asText();
        hacer(post("/api/yo/qr/NOEXISTE"), codAna, "", status().isNotFound());
        hacer(post("/api/yo/qr/" + qr.toLowerCase()), codAna, "", status().isOk());
        assertThat(leer(get("/api/yo"), codAna).get("pistas")).hasSize(1);

        // Habilidad automática: rumor anónimo (los jugadores no ven el autor; el Máster sí)
        long rumor = crear(post("/api/master/jugadores/" + ana + "/habilidades"), master,
                "{\"nombre\":\"Fuentes\",\"descripcion\":\"Publica un rumor\",\"tipo\":\"RUMOR_ANONIMO\"}");
        hacer(post("/api/yo/habilidades/" + rumor), codAna, "{\"texto\":\"Alguien bajó a las doce\"}", status().isOk());
        hacer(post("/api/yo/habilidades/" + rumor), codAna, "{\"texto\":\"Otra vez\"}", status().isConflict());
        JsonNode rumorJugador = leer(get("/api/yo"), codLuis).get("tablon").get(0);
        assertThat(rumorJugador.get("tipo").asText()).isEqualTo("RUMOR");
        assertThat(rumorJugador.get("autor").isNull()).isTrue();
        assertThat(leer(get("/api/master/estado"), master).get("tablon").get(0).get("autor").asText()).isEqualTo("Ana");

        // Habilidad manual: queda pendiente hasta que el Máster contesta; se puede reponer
        long olfato = crear(post("/api/master/jugadores/" + luis + "/habilidades"), master,
                "{\"nombre\":\"Olfato\",\"descripcion\":\"Huele a alguien\",\"tipo\":\"MANUAL\",\"pidePersona\":true}");
        hacer(post("/api/yo/habilidades/" + olfato), codLuis, "{}", status().isBadRequest());
        hacer(post("/api/yo/habilidades/" + olfato), codLuis, "{\"personaId\":" + ana + "}", status().isOk());
        assertThat(leer(get("/api/yo"), codLuis).get("habilidades").get(0).get("estado").asText()).isEqualTo("SOLICITADA");
        sinContenido(post("/api/master/habilidades/" + olfato + "/resolver"), master, "{\"respuesta\":\"Huele a hollín\"}");
        JsonNode resuelta = leer(get("/api/yo"), codLuis).get("habilidades").get(0);
        assertThat(resuelta.get("respuesta").asText()).isEqualTo("Huele a hollín");
        sinContenido(post("/api/master/habilidades/" + olfato + "/resolver"), master, "{\"estado\":\"DISPONIBLE\"}");
        assertThat(leer(get("/api/yo"), codLuis).get("habilidades").get(0).get("estado").asText()).isEqualTo("DISPONIBLE");
    }

    @Test
    void tratosYPistasFalsas() throws Exception {
        String master = crearPartida();
        long ana = crearJugador(master, "Ana", null);
        long luis = crearJugador(master, "Luis", null);
        JsonNode estado = leer(get("/api/master/estado"), master);
        String codAna = codigoDe(estado, ana);
        String codLuis = codigoDe(estado, luis);
        hacer(post("/api/master/dinero"), master, "{\"cantidad\":500}", status().isOk());
        long dePista = crear(post("/api/master/pistas"), master, "{\"titulo\":\"De Ana\",\"contenido\":\"A\"}");
        long luisPista = crear(post("/api/master/pistas"), master, "{\"titulo\":\"De Luis\",\"contenido\":\"L\"}");
        sinContenido(put("/api/master/pistas/" + dePista + "/visibilidad"), master, "{\"jugadorIds\":[" + ana + "]}");
        sinContenido(put("/api/master/pistas/" + luisPista + "/visibilidad"), master, "{\"jugadorIds\":[" + luis + "]}");

        // Ana ofrece su pista y 100 monedas por una pista de Luis
        hacer(post("/api/yo/tratos"), codAna, "{\"paraId\":" + luis + ",\"dinero\":50}", status().isBadRequest());
        long trato = crear(post("/api/yo/tratos"), codAna,
                "{\"paraId\":" + luis + ",\"pistaId\":" + dePista + ",\"dinero\":100,\"pidePista\":true}");
        JsonNode tratoLuis = leer(get("/api/yo"), codLuis).get("tratos").get(0);
        assertThat(tratoLuis.get("puedoDar")).hasSize(1);
        hacer(post("/api/yo/tratos/" + trato + "/aceptar"), codAna, "{\"pistaId\":" + luisPista + "}", status().isNotFound());
        sinContenido(post("/api/yo/tratos/" + trato + "/aceptar"), codLuis, "{\"pistaId\":" + luisPista + "}");
        JsonNode panelAna = leer(get("/api/yo"), codAna);
        JsonNode panelLuis = leer(get("/api/yo"), codLuis);
        assertThat(panelAna.get("pistas")).hasSize(2);
        assertThat(panelLuis.get("pistas")).hasSize(2);
        assertThat(panelAna.get("dinero").asInt()).isEqualTo(400);
        assertThat(panelLuis.get("dinero").asInt()).isEqualTo(600);
        hacer(post("/api/yo/tratos/" + trato + "/rechazar"), codLuis, "", status().isConflict());

        // Pistas falsas: solo el asesino, cuestan 200, se devuelven si el Máster las rechaza
        hacer(post("/api/yo/pistas-falsas"), codAna, "{\"titulo\":\"Falsa\",\"contenido\":\"x\"}", status().isForbidden());
        sinContenido(put("/api/master/jugadores/" + ana + "/asesino"), master, "{\"asesino\":true}");
        long falsa = crear(post("/api/yo/pistas-falsas"), codAna,
                "{\"titulo\":\"Huellas\",\"contenido\":\"Huellas de Luis\",\"paraId\":" + luis + "}");
        assertThat(leer(get("/api/yo"), codAna).get("dinero").asInt()).isEqualTo(200);
        assertThat(leer(get("/api/yo"), codLuis).get("pistas")).hasSize(2);
        sinContenido(post("/api/master/pistas/" + falsa + "/falsa/aprobar"), master, "{}");
        JsonNode pistasLuis = leer(get("/api/yo"), codLuis).get("pistas");
        assertThat(pistasLuis).hasSize(3);
        assertThat(pistasLuis.toString()).doesNotContain("falsa");

        long otra = crear(post("/api/yo/pistas-falsas"), codAna, "{\"titulo\":\"Otra\",\"contenido\":\"y\"}");
        assertThat(leer(get("/api/yo"), codAna).get("dinero").asInt()).isZero();
        sinContenido(post("/api/master/pistas/" + otra + "/falsa/rechazar"), master, "");
        JsonNode asesinato = leer(get("/api/yo"), codAna);
        assertThat(asesinato.get("dinero").asInt()).isEqualTo(200);
        assertThat(asesinato.get("asesinato").get("pistasFalsas")).hasSize(2);
    }

    @Test
    void sobresProgramadosYVisiones() throws Exception {
        String master = crearPartida();
        long ana = crearJugador(master, "Ana", null);
        String codAna = codigoDe(leer(get("/api/master/estado"), master), ana);
        long pista = crear(post("/api/master/pistas"), master, "{\"titulo\":\"Sobre\",\"contenido\":\"Dentro\"}");

        // Sobre anunciado para dentro de una hora: se ve sellado, sin contenido
        long sobre = crear(post("/api/master/envios"), master, "{\"tipo\":\"PISTA\",\"pistaId\":" + pista
                + ",\"jugadorIds\":[" + ana + "],\"programadaPara\":\"" + Instant.now().plusSeconds(3600) + "\",\"anunciar\":true}");
        JsonNode panel = leer(get("/api/yo"), codAna);
        assertThat(panel.get("sobresSellados")).hasSize(1);
        assertThat(panel.get("pistas")).isEmpty();

        // Al llegar la hora se abre solo
        sinContenido(put("/api/master/envios/" + sobre), master, "{\"tipo\":\"PISTA\",\"pistaId\":" + pista
                + ",\"jugadorIds\":[" + ana + "],\"programadaPara\":\"" + Instant.now().minusSeconds(5) + "\",\"anunciar\":true}");
        mecanicas.entregarPendientes();
        panel = leer(get("/api/yo"), codAna);
        assertThat(panel.get("sobresSellados")).isEmpty();
        assertThat(panel.get("pistas").get(0).get("titulo").asText()).isEqualTo("Sobre");

        // Visión de reserva sin imagen: no se puede mandar; con imagen se manda una copia
        long vision = crear(post("/api/master/envios"), master, "{\"tipo\":\"VISION\",\"nota\":\"Un reloj parado\"}");
        hacer(post("/api/master/envios/" + vision + "/enviar"), master, "{\"jugadorIds\":[" + ana + "]}", status().isBadRequest());
        sinContenido(put("/api/master/envios/" + vision), master,
                "{\"tipo\":\"VISION\",\"nota\":\"Un reloj parado\",\"imagenUrl\":\"https://ejemplo.org/reloj.jpg\"}");
        hacer(post("/api/master/envios/" + vision + "/enviar"), master, "{\"jugadorIds\":[" + ana + "]}", status().isOk());
        JsonNode visiones = leer(get("/api/yo"), codAna).get("visiones");
        assertThat(visiones).hasSize(1);
        assertThat(visiones.get(0).get("imagenUrl").asText()).isEqualTo("https://ejemplo.org/reloj.jpg");
        assertThat(visiones.toString()).doesNotContain("reloj parado");
        // La de reserva sigue disponible para otros
        assertThat(leer(get("/api/master/estado"), master).get("envios")).hasSize(3);
    }

    @Test
    void imagenesSubidasSeUsanEnVisiones() throws Exception {
        String master = crearPartida();
        long ana = crearJugador(master, "Ana", null);
        String codAna = codigoDe(leer(get("/api/master/estado"), master), ana);
        byte[] png = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

        // Solo el Máster sube, y solo imágenes
        mvc.perform(multipart("/api/master/imagenes").file(new org.springframework.mock.web.MockMultipartFile("archivo", "v.png", "image/png", png))
                .header("Authorization", "Bearer " + codAna)).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/master/imagenes").file(new org.springframework.mock.web.MockMultipartFile("archivo", "x.txt", "text/plain", png))
                .header("Authorization", "Bearer " + master)).andExpect(status().isBadRequest());
        String url = json.readTree(mvc.perform(multipart("/api/master/imagenes")
                        .file(new org.springframework.mock.web.MockMultipartFile("archivo", "carpeta/v.png", "image/png", png))
                        .header("Authorization", "Bearer " + master))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("url").asText();
        assertThat(url).startsWith("/api/imagenes/");
        // Volver a subir el mismo nombre la reemplaza sin cambiar el enlace
        String otra = json.readTree(mvc.perform(multipart("/api/master/imagenes")
                        .file(new org.springframework.mock.web.MockMultipartFile("archivo", "v.png", "image/png", png))
                        .header("Authorization", "Bearer " + master))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("url").asText();
        assertThat(otra).isEqualTo(url);
        assertThat(leer(get("/api/master/imagenes"), master)).hasSize(1);

        // Se ve sin código (va en un <img>) y una clave inventada no da nada
        byte[] servida = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(servida).isEqualTo(png);
        mvc.perform(get("/api/imagenes/NOEXISTE")).andExpect(status().isNotFound());

        // Y vale como imagen de una visión
        long vision = crear(post("/api/master/envios"), master,
                "{\"tipo\":\"VISION\",\"imagenUrl\":\"" + url + "\",\"jugadorIds\":[" + ana + "]}");
        hacer(post("/api/master/envios/" + vision + "/enviar"), master, "{}", status().isOk());
        assertThat(leer(get("/api/yo"), codAna).get("visiones").get(0).get("imagenUrl").asText()).isEqualTo(url);
    }

    // ---------------------------------------------------------------- utilidades

    private String crearPartida() throws Exception {
        String respuesta = mvc.perform(post("/api/admin/partidas").header("X-Admin-Key", "clave-de-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Mecánicas\",\"nombreMaster\":\"Master\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("codigoMaster").asText();
    }

    private long crearJugador(String master, String nombre, String herramienta) throws Exception {
        return crear(post("/api/master/jugadores"), master, "{\"nombre\":\"" + nombre + "\""
                + (herramienta == null ? "" : ",\"herramienta\":\"" + herramienta + "\"") + "}");
    }

    private long crear(MockHttpServletRequestBuilder peticion, String codigo, String cuerpo) throws Exception {
        String respuesta = mvc.perform(peticion.header("Authorization", "Bearer " + codigo)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("id").asLong();
    }

    private void sinContenido(MockHttpServletRequestBuilder peticion, String codigo, String cuerpo) throws Exception {
        hacer(peticion, codigo, cuerpo, status().isNoContent());
    }

    private void hacer(MockHttpServletRequestBuilder peticion, String codigo, String cuerpo, ResultMatcher esperado) throws Exception {
        peticion.header("Authorization", "Bearer " + codigo);
        if (!cuerpo.isEmpty()) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        mvc.perform(peticion).andExpect(esperado);
    }

    private JsonNode leer(MockHttpServletRequestBuilder peticion, String codigo) throws Exception {
        String respuesta = mvc.perform(peticion.header("Authorization", "Bearer " + codigo))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta);
    }

    private static String codigoDe(JsonNode estado, long jugadorId) {
        for (JsonNode jugador : estado.get("jugadores")) {
            if (jugador.get("id").asLong() == jugadorId) {
                return jugador.get("codigoAcceso").asText();
            }
        }
        throw new AssertionError("Jugador no encontrado: " + jugadorId);
    }

    private static JsonNode filaRanking(JsonNode estado, long jugadorId) {
        for (JsonNode fila : estado.get("ranking")) {
            if (fila.get("jugadorId").asLong() == jugadorId) {
                return fila;
            }
        }
        throw new AssertionError("Sin fila de ranking: " + jugadorId);
    }
}
