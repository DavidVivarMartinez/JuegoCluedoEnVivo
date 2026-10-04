package com.misterioenvivo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recorre una partida completa con datos inventados y comprueba quién ve qué. */
@SpringBootTest
@AutoConfigureMockMvc
class FlujoPartidaTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    @Test
    void crearPartidaExigeClaveDeAdministracion() throws Exception {
        String cuerpo = "{\"nombre\":\"Prueba\",\"nombreMaster\":\"Master\"}";
        mvc.perform(post("/api/admin/partidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/partidas").header("X-Admin-Key", "mala")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinCodigoNoSeEntra() throws Exception {
        mvc.perform(get("/api/yo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/master/estado").header("Authorization", "Bearer NOEXISTE"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/sesion").contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"NOEXISTE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void flujoCompletoDeUnaPartida() throws Exception {
        String master = crearPartida("Partida A");

        long ana = crearJugador(master, "Ana");
        long luis = crearJugador(master, "Luis");

        JsonNode estado = leer(get("/api/master/estado"), master);
        assertThat(estado.get("jugadores")).hasSize(2);
        assertThat(estado.get("partida").get("fase").asText()).isEqualTo("PREPARACION");
        String codigoAna = codigoDe(estado, ana);
        String codigoLuis = codigoDe(estado, luis);

        // La sesión dice a cada uno qué panel le toca
        JsonNode sesionAna = json.readTree(mvc.perform(post("/api/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigoAna.toLowerCase() + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(sesionAna.get("rol").asText()).isEqualTo("JUGADOR");
        assertThat(sesionAna.get("nombre").asText()).isEqualTo("Ana");

        // Un jugador no puede usar el panel del Máster
        mvc.perform(get("/api/master/estado").header("Authorization", "Bearer " + codigoAna))
                .andExpect(status().isForbidden());

        // Secreto de Ana, objetivo de Ana, pista solo para Ana
        long secreto = crear(post("/api/master/jugadores/" + ana + "/secretos"), master, "{\"texto\":\"Secreto de Ana\"}");
        crear(post("/api/master/jugadores/" + ana + "/objetivos"), master, "{\"texto\":\"Ocultar su secreto\"}");
        long pista = crear(post("/api/master/pistas"), master, "{\"titulo\":\"Nota\",\"contenido\":\"Una nota rota\"}");
        enviar(put("/api/master/pistas/" + pista + "/visibilidad"), master, "{\"jugadorIds\":[" + ana + "]}");

        // Fase y asesino los decide el Máster durante el juego
        enviar(put("/api/master/fase"), master, "{\"fase\":\"INVESTIGACION\"}");
        enviar(put("/api/master/asesino"), master, "{\"jugadorId\":" + ana + "}");

        // Mensaje a todos y variable narrativa
        mvc.perform(post("/api/master/mensajes").header("Authorization", "Bearer " + master)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Empieza la investigación\"}"))
                .andExpect(status().isCreated());
        enviar(put("/api/master/variables/arma"), master, "{\"valor\":\"pendiente\"}");

        JsonNode panelAna = leer(get("/api/yo"), codigoAna);
        assertThat(panelAna.get("esAsesino").asBoolean()).isTrue();
        assertThat(panelAna.get("partida").get("fase").asText()).isEqualTo("INVESTIGACION");
        assertThat(panelAna.get("secretos")).hasSize(1);
        assertThat(panelAna.get("objetivos")).hasSize(1);
        assertThat(panelAna.get("pistas")).hasSize(1);
        assertThat(panelAna.get("mensajes")).hasSize(1);
        assertThat(panelAna.get("participantes")).hasSize(3);

        // Luis no ve nada de Ana ni sabe quién es el asesino
        JsonNode panelLuis = leer(get("/api/yo"), codigoLuis);
        assertThat(panelLuis.get("esAsesino").asBoolean()).isFalse();
        assertThat(panelLuis.get("secretos")).isEmpty();
        assertThat(panelLuis.get("pistas")).isEmpty();
        assertThat(panelLuis.get("secretosRevelados")).isEmpty();
        assertThat(panelLuis.get("mensajes")).hasSize(1);
        assertThat(panelLuis.toString()).doesNotContain("Secreto de Ana").doesNotContain("asesinoId")
                .doesNotContain("codigoAcceso");

        // Al revelar el secreto, Luis lo ve
        enviar(put("/api/master/secretos/" + secreto), master, "{\"revelado\":true}");
        panelLuis = leer(get("/api/yo"), codigoLuis);
        assertThat(panelLuis.get("secretosRevelados")).hasSize(1);
        assertThat(panelLuis.get("secretosRevelados").get(0).get("jugador").asText()).isEqualTo("Ana");

        // Marcar mensaje como leído: solo el propio
        long mensajeAna = panelAna.get("mensajes").get(0).get("id").asLong();
        mvc.perform(post("/api/yo/mensajes/" + mensajeAna + "/leido").header("Authorization", "Bearer " + codigoLuis))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/yo/mensajes/" + mensajeAna + "/leido").header("Authorization", "Bearer " + codigoAna))
                .andExpect(status().isNoContent());
        assertThat(leer(get("/api/yo"), codigoAna).get("mensajes").get(0).get("leido").asBoolean()).isTrue();

        estado = leer(get("/api/master/estado"), master);
        assertThat(estado.get("asesinoId").asLong()).isEqualTo(ana);
        assertThat(estado.get("variables").get(0).get("clave").asText()).isEqualTo("arma");
        assertThat(estado.get("eventos").size()).isGreaterThanOrEqualTo(5);

        // Borrar a la asesina limpia todo lo suyo
        mvc.perform(delete("/api/master/jugadores/" + ana).header("Authorization", "Bearer " + master))
                .andExpect(status().isNoContent());
        estado = leer(get("/api/master/estado"), master);
        assertThat(estado.get("jugadores")).hasSize(1);
        assertThat(estado.get("asesinoId").isNull()).isTrue();
        assertThat(estado.get("pistas").get(0).get("jugadorIds")).isEmpty();
        mvc.perform(get("/api/yo").header("Authorization", "Bearer " + codigoAna))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lasPartidasEstanAisladasEntreSi() throws Exception {
        String masterA = crearPartida("Casa A");
        String masterB = crearPartida("Casa B");
        long jugadorA = crearJugador(masterA, "Jugador de A");
        long pistaA = crear(post("/api/master/pistas"), masterA, "{\"titulo\":\"T\",\"contenido\":\"C\"}");

        // El Máster de B no puede tocar nada de A
        mvc.perform(put("/api/master/asesino").header("Authorization", "Bearer " + masterB)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"jugadorId\":" + jugadorA + "}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/master/jugadores/" + jugadorA).header("Authorization", "Bearer " + masterB))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/master/pistas/" + pistaA).header("Authorization", "Bearer " + masterB))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/master/jugadores/" + jugadorA + "/secretos").header("Authorization", "Bearer " + masterB)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"x\"}"))
                .andExpect(status().isNotFound());

        assertThat(leer(get("/api/master/estado"), masterB).get("jugadores")).isEmpty();
        assertThat(leer(get("/api/master/estado"), masterA).get("jugadores")).hasSize(1);
    }

    // ---------- utilidades ----------

    private String crearPartida(String nombre) throws Exception {
        String respuesta = mvc.perform(post("/api/admin/partidas").header("X-Admin-Key", "clave-de-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"casa\":\"Casa de prueba\","
                                + "\"fechaInicio\":\"2026-10-23\",\"fechaFin\":\"2026-10-25\",\"nombreMaster\":\"Master\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("codigoMaster").asText();
    }

    private long crearJugador(String master, String nombre) throws Exception {
        return crear(post("/api/master/jugadores"), master, "{\"nombre\":\"" + nombre + "\"}");
    }

    private long crear(MockHttpServletRequestBuilder peticion, String codigo, String cuerpo) throws Exception {
        String respuesta = mvc.perform(peticion.header("Authorization", "Bearer " + codigo)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("id").asLong();
    }

    private void enviar(MockHttpServletRequestBuilder peticion, String codigo, String cuerpo) throws Exception {
        mvc.perform(peticion.header("Authorization", "Bearer " + codigo)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isNoContent());
    }

    private JsonNode leer(MockHttpServletRequestBuilder peticion, String codigo) throws Exception {
        String respuesta = mvc.perform(peticion.header("Authorization", "Bearer " + codigo))
                .andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
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
}
