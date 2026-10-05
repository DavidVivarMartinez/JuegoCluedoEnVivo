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

        // Secreto de Ana, objetivos de Ana, pista solo para Ana
        long secreto = crear(post("/api/master/jugadores/" + ana + "/secretos"), master, "{\"texto\":\"Secreto de Ana\"}");
        long secundario = crear(post("/api/master/jugadores/" + ana + "/objetivos"), master, "{\"texto\":\"Ocultar su secreto\"}");
        crear(post("/api/master/jugadores/" + ana + "/objetivos"), master, "{\"texto\":\"Encontrar al asesino\",\"principal\":true}");
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
        assertThat(panelAna.get("objetivos")).hasSize(2);
        assertThat(panelAna.get("objetivos").get(0).get("principal").asBoolean()).isFalse();
        assertThat(panelAna.get("objetivos").get(1).get("principal").asBoolean()).isTrue();
        assertThat(panelAna.get("pistas")).hasSize(1);
        assertThat(panelAna.get("mensajes")).hasSize(1);
        // De los demás (el Máster incluido, que también es un personaje) solo llega la ficha pública
        assertThat(panelAna.get("otrosPersonajes")).hasSize(2);
        assertThat(panelAna.get("otrosPersonajes").get(0).get("nombre").asText()).isEqualTo("Luis");
        assertThat(panelAna.get("otrosPersonajes").get(0).get("esMaster").asBoolean()).isFalse();
        assertThat(panelAna.get("otrosPersonajes").get(1).get("nombre").asText()).isEqualTo("Master");
        assertThat(panelAna.get("otrosPersonajes").get(1).get("esMaster").asBoolean()).isTrue();
        assertThat(panelAna.get("otrosPersonajes").get(0).has("personalidad")).isFalse();
        long masterId = panelAna.get("otrosPersonajes").get(1).get("id").asLong();

        // Solo hay un objetivo principal: al cambiarlo, el anterior pasa a secundario
        enviar(put("/api/master/objetivos/" + secundario), master, "{\"principal\":true}");
        JsonNode objetivosAna = leer(get("/api/yo"), codigoAna).get("objetivos");
        assertThat(objetivosAna.get(0).get("principal").asBoolean()).isTrue();
        assertThat(objetivosAna.get(1).get("principal").asBoolean()).isFalse();

        // Objetivo con pruebas: Ana debe señalar a dos personas; al cumplirlo se desbloquea una pista
        long premio = crear(post("/api/master/pistas"), master, "{\"titulo\":\"Premio\",\"contenido\":\"La llave del desván\"}");
        long investigar = crear(post("/api/master/jugadores/" + ana + "/objetivos"), master,
                "{\"texto\":\"Averigua qué hicieron dos personas\",\"tipo\":\"PERSONA\",\"cantidad\":2}");
        enviar(put("/api/master/objetivos/" + investigar + "/recompensa"), master, "{\"pistaId\":" + premio + "}");

        String rutaEntregas = "/api/yo/objetivos/" + investigar + "/entregas";
        mvc.perform(post(rutaEntregas).header("Authorization", "Bearer " + codigoLuis) // no es suyo
                        .contentType(MediaType.APPLICATION_JSON).content("{\"personaId\":" + ana + "}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(rutaEntregas).header("Authorization", "Bearer " + codigoAna) // falta la persona
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"sin nadie\"}"))
                .andExpect(status().isBadRequest());
        crear(post(rutaEntregas), codigoAna, "{\"personaId\":" + luis + ",\"texto\":\"Estuvo en la cocina\"}");
        JsonNode objetivo = objetivo(leer(get("/api/yo"), codigoAna), investigar);
        assertThat(objetivo.get("estado").asText()).isEqualTo("ACTIVO");
        assertThat(objetivo.get("entregas")).hasSize(1);
        assertThat(objetivo.get("entregas").get(0).get("persona").asText()).isEqualTo("Luis");
        assertThat(objetivo.get("tieneRecompensa").asBoolean()).isTrue();
        assertThat(objetivo.has("recompensaPistaId")).isFalse();

        crear(post(rutaEntregas), codigoAna, "{\"personaId\":" + masterId + ",\"texto\":\"No se movió del salón\"}");
        panelAna = leer(get("/api/yo"), codigoAna);
        assertThat(objetivo(panelAna, investigar).get("estado").asText()).isEqualTo("ENTREGADO");
        assertThat(panelAna.get("pistas")).hasSize(1); // el premio aún no

        // El Máster lo ve pendiente, con las pruebas, y lo valida: la pista aparece
        JsonNode objetivoMaster = objetivoDe(leer(get("/api/master/estado"), master), ana, investigar);
        assertThat(objetivoMaster.get("estado").asText()).isEqualTo("ENTREGADO");
        assertThat(objetivoMaster.get("entregas")).hasSize(2);
        assertThat(objetivoMaster.get("recompensaPistaId").asLong()).isEqualTo(premio);
        enviar(put("/api/master/objetivos/" + investigar), master, "{\"estado\":\"CUMPLIDO\"}");
        panelAna = leer(get("/api/yo"), codigoAna);
        assertThat(panelAna.get("pistas")).hasSize(2);
        assertThat(objetivo(panelAna, investigar).get("estado").asText()).isEqualTo("CUMPLIDO");
        mvc.perform(post(rutaEntregas).header("Authorization", "Bearer " + codigoAna) // cerrado
                        .contentType(MediaType.APPLICATION_JSON).content("{\"personaId\":" + luis + "}"))
                .andExpect(status().isConflict());

        // Un logro se marca una vez (repetirlo solo actualiza la nota) y queda pendiente de validar
        long logro = crear(post("/api/master/jugadores/" + luis + "/objetivos"), master,
                "{\"texto\":\"Llega puntual a todas las comidas\",\"tipo\":\"LOGRO\"}");
        crear(post("/api/yo/objetivos/" + logro + "/entregas"), codigoLuis, "{\"texto\":\"Ni un minuto tarde\"}");
        crear(post("/api/yo/objetivos/" + logro + "/entregas"), codigoLuis, "{\"texto\":\"Lo prometo\"}");
        JsonNode logroLuis = objetivo(leer(get("/api/yo"), codigoLuis), logro);
        assertThat(logroLuis.get("estado").asText()).isEqualTo("ENTREGADO");
        assertThat(logroLuis.get("entregas")).hasSize(1);
        assertThat(logroLuis.get("entregas").get(0).get("texto").asText()).isEqualTo("Lo prometo");
        // Si retira la prueba, vuelve a estar activo
        long entregaLogro = logroLuis.get("entregas").get(0).get("id").asLong();
        mvc.perform(delete("/api/yo/entregas/" + entregaLogro).header("Authorization", "Bearer " + codigoAna)) // no es suya
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/yo/entregas/" + entregaLogro).header("Authorization", "Bearer " + codigoLuis))
                .andExpect(status().isNoContent());
        assertThat(objetivo(leer(get("/api/yo"), codigoLuis), logro).get("estado").asText()).isEqualTo("ACTIVO");

        // Luis no ve nada de Ana ni sabe quién es el asesino
        JsonNode panelLuis = leer(get("/api/yo"), codigoLuis);
        assertThat(panelLuis.get("esAsesino").asBoolean()).isFalse();
        assertThat(panelLuis.get("secretos")).isEmpty();
        assertThat(panelLuis.get("pistas")).isEmpty();
        assertThat(panelLuis.get("secretosRevelados")).isEmpty();
        assertThat(panelLuis.get("mensajes")).hasSize(1);
        assertThat(panelLuis.toString()).doesNotContain("Secreto de Ana").doesNotContain("asesinoId")
                .doesNotContain("codigoAcceso").doesNotContain("Estuvo en la cocina");

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
        assertThat(estado.get("asesinoIds")).hasSize(1);
        assertThat(estado.get("asesinoIds").get(0).asLong()).isEqualTo(ana);
        assertThat(estado.get("variables").get(0).get("clave").asText()).isEqualTo("arma");
        assertThat(estado.get("eventos").size()).isGreaterThanOrEqualTo(5);

        // Luis señala a Ana en un objetivo suyo; al borrar a Ana, la prueba queda sin persona
        long vigilar = crear(post("/api/master/jugadores/" + luis + "/objetivos"), master,
                "{\"texto\":\"Vigila a alguien\",\"tipo\":\"PERSONA\"}");
        crear(post("/api/yo/objetivos/" + vigilar + "/entregas"), codigoLuis, "{\"personaId\":" + ana + ",\"texto\":\"Salió al jardín\"}");
        enviarYLeer(put("/api/yo/anotaciones/" + ana), codigoLuis, "{\"texto\":\"Ana se puso nerviosa\"}");

        // Borrar a la asesina limpia todo lo suyo, incluidas las notas que hablaban de ella
        mvc.perform(delete("/api/master/jugadores/" + ana).header("Authorization", "Bearer " + master))
                .andExpect(status().isNoContent());
        estado = leer(get("/api/master/estado"), master);
        assertThat(estado.get("jugadores")).hasSize(1);
        assertThat(estado.get("asesinoIds")).isEmpty();
        assertThat(estado.get("pistas").get(0).get("jugadorIds")).isEmpty();
        mvc.perform(get("/api/yo").header("Authorization", "Bearer " + codigoAna))
                .andExpect(status().isUnauthorized());
        panelLuis = leer(get("/api/yo"), codigoLuis);
        JsonNode otrosDeLuis = panelLuis.get("otrosPersonajes");
        assertThat(otrosDeLuis).hasSize(1);
        assertThat(otrosDeLuis.get(0).get("nombre").asText()).isEqualTo("Master");
        JsonNode entregaHuerfana = objetivo(panelLuis, vigilar).get("entregas").get(0);
        assertThat(entregaHuerfana.get("persona").isNull()).isTrue();
        assertThat(entregaHuerfana.get("texto").asText()).isEqualTo("Salió al jardín");
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

    @Test
    void lasAnotacionesSonPrivadasDeCadaJugador() throws Exception {
        String master = crearPartida("Partida con cuaderno");
        long ana = crearJugador(master, "Ana");
        long luis = crearJugador(master, "Luis");
        JsonNode estado = leer(get("/api/master/estado"), master);
        String codigoAna = codigoDe(estado, ana);
        String codigoLuis = codigoDe(estado, luis);

        // La foto forma parte de la ficha pública y debe ser un enlace http(s)
        mvc.perform(put("/api/master/jugadores/" + luis).header("Authorization", "Bearer " + master)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Luis\",\"imagenUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());
        enviar(put("/api/master/jugadores/" + luis), master,
                "{\"nombre\":\"Luis\",\"edad\":41,\"profesion\":\"Notario\",\"imagenUrl\":\"https://fotos.test/luis.jpg\","
                        + "\"personalidad\":\"Reservado\"}");

        // Ana anota sobre Luis y lo recupera; de Luis solo ve la ficha pública
        JsonNode guardada = enviarYLeer(put("/api/yo/anotaciones/" + luis), codigoAna,
                "{\"texto\":\"  Luis miente sobre la cena  \"}");
        assertThat(guardada.get("texto").asText()).isEqualTo("Luis miente sobre la cena");
        assertThat(guardada.get("actualizadoEn").isNull()).isFalse();

        JsonNode luisParaAna = leer(get("/api/yo"), codigoAna).get("otrosPersonajes").get(0);
        assertThat(luisParaAna.get("nombre").asText()).isEqualTo("Luis");
        assertThat(luisParaAna.get("profesion").asText()).isEqualTo("Notario");
        assertThat(luisParaAna.get("imagenUrl").asText()).isEqualTo("https://fotos.test/luis.jpg");
        assertThat(luisParaAna.get("anotacion").asText()).isEqualTo("Luis miente sobre la cena");
        assertThat(luisParaAna.toString()).doesNotContain("Reservado");

        // Ni Luis ni el Máster ven la nota de Ana
        JsonNode panelLuis = leer(get("/api/yo"), codigoLuis);
        assertThat(panelLuis.get("otrosPersonajes").get(0).get("anotacion").isNull()).isTrue();
        assertThat(panelLuis.toString()).doesNotContain("Luis miente");
        assertThat(leer(get("/api/master/estado"), master).toString()).doesNotContain("Luis miente");

        // Solo se anota sobre otros personajes de la misma partida
        mvc.perform(put("/api/yo/anotaciones/" + ana).header("Authorization", "Bearer " + codigoAna)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"yo\"}"))
                .andExpect(status().isBadRequest());
        String masterB = crearPartida("Otra casa");
        long ajeno = crearJugador(masterB, "Ajeno");
        mvc.perform(put("/api/yo/anotaciones/" + ajeno).header("Authorization", "Bearer " + codigoAna)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"x\"}"))
                .andExpect(status().isNotFound());

        // Vaciar la nota la deja en blanco
        JsonNode vacia = enviarYLeer(put("/api/yo/anotaciones/" + luis), codigoAna, "{\"texto\":\"\"}");
        assertThat(vacia.get("texto").asText()).isEmpty();

        // El Máster también es un personaje: edita su ficha pública y los jugadores pueden anotar sobre él
        enviar(put("/api/master/ficha"), master,
                "{\"nombre\":\"David\",\"profesion\":\"Anfitrión\",\"imagenUrl\":\"https://fotos.test/david.jpg\"}");
        JsonNode davidParaAna = leer(get("/api/yo"), codigoAna).get("otrosPersonajes").get(0); // "David" < "Luis"
        assertThat(davidParaAna.get("nombre").asText()).isEqualTo("David");
        assertThat(davidParaAna.get("esMaster").asBoolean()).isTrue();
        assertThat(davidParaAna.get("profesion").asText()).isEqualTo("Anfitrión");
        assertThat(davidParaAna.get("imagenUrl").asText()).isEqualTo("https://fotos.test/david.jpg");
        long masterId = davidParaAna.get("id").asLong();
        JsonNode sobreDavid = enviarYLeer(put("/api/yo/anotaciones/" + masterId), codigoAna, "{\"texto\":\"Sabe más de lo que dice\"}");
        assertThat(sobreDavid.get("texto").asText()).isEqualTo("Sabe más de lo que dice");
        JsonNode estadoMaster = leer(get("/api/master/estado"), master);
        assertThat(estadoMaster.get("master").get("nombre").asText()).isEqualTo("David");
        assertThat(estadoMaster.get("jugadores")).hasSize(2); // el Máster no cuenta como jugador
        assertThat(estadoMaster.toString()).doesNotContain("Sabe más de lo que dice");
    }

    @Test
    void dineroTiendaYVariosAsesinosConArma() throws Exception {
        String master = crearPartida("Partida con dinero");
        long ana = crearJugador(master, "Ana");
        long luis = crearJugador(master, "Luis");
        long eva = crearJugador(master, "Eva");
        JsonNode estado = leer(get("/api/master/estado"), master);
        String codigoAna = codigoDe(estado, ana);
        String codigoLuis = codigoDe(estado, luis);
        String codigoEva = codigoDe(estado, eva);

        // Herramientas de oficio (públicas)
        enviar(put("/api/master/jugadores/" + ana), master, "{\"nombre\":\"Ana\",\"profesion\":\"Fontanera\",\"herramienta\":\"Llave inglesa\"}");
        enviar(put("/api/master/jugadores/" + luis), master, "{\"nombre\":\"Luis\",\"profesion\":\"Cocinero\",\"herramienta\":\"Rodillo de amasar\"}");
        assertThat(leer(get("/api/yo"), codigoEva).toString()).contains("Rodillo de amasar");

        // El Máster reparte 100 a todos y quita 30 a Luis; nunca puede quedar en negativo
        dinero(master, "{\"cantidad\":100,\"concepto\":\"Dinero inicial\"}", 200);
        dinero(master, "{\"cantidad\":-30,\"jugadorIds\":[" + luis + "]}", 200);
        dinero(master, "{\"cantidad\":-500,\"jugadorIds\":[" + luis + "]}", 400);
        assertThat(leer(get("/api/yo"), codigoLuis).get("dinero").asInt()).isEqualTo(70);

        // Pagos entre jugadores
        pagar(codigoAna, "{\"jugadorId\":" + luis + ",\"cantidad\":25,\"concepto\":\"Por callar\"}", 201);
        pagar(codigoAna, "{\"jugadorId\":" + luis + ",\"cantidad\":1000}", 400); // sin saldo
        pagar(codigoAna, "{\"jugadorId\":" + ana + ",\"cantidad\":5}", 400);     // a sí misma
        pagar(codigoAna, "{\"jugadorId\":" + luis + ",\"cantidad\":-5}", 400);   // negativo
        JsonNode panelAna = leer(get("/api/yo"), codigoAna);
        assertThat(panelAna.get("dinero").asInt()).isEqualTo(75);
        assertThat(panelAna.get("movimientos").get(0).get("para").asText()).isEqualTo("Luis");
        assertThat(leer(get("/api/yo"), codigoLuis).get("dinero").asInt()).isEqualTo(95);
        assertThat(leer(get("/api/yo"), codigoEva).toString()).doesNotContain("Por callar");

        // Tienda: se ve el título y el precio, no el contenido, hasta comprarla
        long pista = crear(post("/api/master/pistas"), master, "{\"titulo\":\"Recibo\",\"contenido\":\"Un recibo de las 23:50\"}");
        enviar(put("/api/master/pistas/" + pista + "/precio"), master, "{\"precio\":60}");
        panelAna = leer(get("/api/yo"), codigoAna);
        assertThat(panelAna.get("tienda")).hasSize(1);
        assertThat(panelAna.get("tienda").get(0).get("precio").asInt()).isEqualTo(60);
        assertThat(panelAna.toString()).doesNotContain("Un recibo de las 23:50");
        JsonNode comprada = json.readTree(mvc.perform(post("/api/yo/tienda/" + pista).header("Authorization", "Bearer " + codigoAna))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(comprada.get("contenido").asText()).isEqualTo("Un recibo de las 23:50");
        panelAna = leer(get("/api/yo"), codigoAna);
        assertThat(panelAna.get("dinero").asInt()).isEqualTo(15);
        assertThat(panelAna.get("pistas")).hasSize(1);
        assertThat(panelAna.get("tienda")).isEmpty();
        mvc.perform(post("/api/yo/tienda/" + pista).header("Authorization", "Bearer " + codigoAna)) // ya la tiene
                .andExpect(status().isConflict());
        mvc.perform(post("/api/yo/tienda/" + pista).header("Authorization", "Bearer " + codigoEva))
                .andExpect(status().isOk());

        // Objetivo con recompensa en dinero
        long objetivo = crear(post("/api/master/jugadores/" + eva + "/objetivos"), master,
                "{\"texto\":\"Haz algo\",\"tipo\":\"LOGRO\",\"recompensaDinero\":40}");
        assertThat(objetivo(leer(get("/api/yo"), codigoEva), objetivo).get("recompensaDinero").asInt()).isEqualTo(40);
        enviar(put("/api/master/objetivos/" + objetivo), master, "{\"estado\":\"CUMPLIDO\"}");
        assertThat(leer(get("/api/yo"), codigoEva).get("dinero").asInt()).isEqualTo(80); // 100 - 60 + 40

        // Dos asesinos: se ven como cómplices; un jugador normal no sabe nada
        enviar(put("/api/master/jugadores/" + ana + "/asesino"), master, "{\"asesino\":true}");
        enviar(put("/api/master/jugadores/" + luis + "/asesino"), master, "{\"asesino\":true}");
        assertThat(leer(get("/api/master/estado"), master).get("asesinoIds")).hasSize(2);
        panelAna = leer(get("/api/yo"), codigoAna);
        assertThat(panelAna.get("esAsesino").asBoolean()).isTrue();
        assertThat(panelAna.get("asesinato").get("complices").get(0).asText()).isEqualTo("Luis");
        JsonNode panelEva = leer(get("/api/yo"), codigoEva);
        assertThat(panelEva.get("esAsesino").asBoolean()).isFalse();
        assertThat(panelEva.get("asesinato").isNull()).isTrue();

        // Arma: Ana usa la herramienta de Luis para incriminarle; Eva (no asesina) no puede elegir
        enviar(put("/api/yo/arma"), codigoAna, "{\"jugadorId\":" + luis + "}");
        enviar(put("/api/yo/arma"), codigoLuis, "{\"jugadorId\":" + luis + "}");
        mvc.perform(put("/api/yo/arma").header("Authorization", "Bearer " + codigoEva)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"jugadorId\":" + ana + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/yo/arma").header("Authorization", "Bearer " + codigoAna) // Eva no tiene herramienta
                        .contentType(MediaType.APPLICATION_JSON).content("{\"jugadorId\":" + eva + "}"))
                .andExpect(status().isBadRequest());
        assertThat(leer(get("/api/yo"), codigoAna).get("asesinato").get("armaDeId").asLong()).isEqualTo(luis);

        // El Máster genera la pista del arma: señala el oficio, no el nombre, y queda bloqueada
        long pistaArma = json.readTree(mvc.perform(post("/api/master/arma/pista").header("Authorization", "Bearer " + master))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        JsonNode pistaGenerada = null;
        for (JsonNode p : leer(get("/api/master/estado"), master).get("pistas")) {
            if (p.get("id").asLong() == pistaArma) {
                pistaGenerada = p;
            }
        }
        assertThat(pistaGenerada.get("titulo").asText()).isEqualTo("El arma");
        assertThat(pistaGenerada.get("contenido").asText()).contains("rodillo de amasar").contains("cocinero").doesNotContain("Luis");
        assertThat(pistaGenerada.get("jugadorIds")).isEmpty();

        // En investigación el arma queda bloqueada
        enviar(put("/api/master/fase"), master, "{\"fase\":\"INVESTIGACION\"}");
        assertThat(leer(get("/api/yo"), codigoAna).get("asesinato").get("armaBloqueada").asBoolean()).isTrue();
        mvc.perform(put("/api/yo/arma").header("Authorization", "Bearer " + codigoAna)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"jugadorId\":" + ana + "}"))
                .andExpect(status().isConflict());

        // Borrar a Luis: Ana se queda sin arma ni cómplice, y el historial de dinero se conserva
        mvc.perform(delete("/api/master/jugadores/" + luis).header("Authorization", "Bearer " + master))
                .andExpect(status().isNoContent());
        panelAna = leer(get("/api/yo"), codigoAna);
        assertThat(panelAna.get("asesinato").get("armaDeId").isNull()).isTrue();
        assertThat(panelAna.get("asesinato").get("complices")).isEmpty();
        assertThat(panelAna.get("movimientos").toString()).contains("Luis");
        assertThat(leer(get("/api/master/estado"), master).get("transacciones").size()).isGreaterThan(5);
    }

    private void dinero(String master, String cuerpo, int esperado) throws Exception {
        mvc.perform(post("/api/master/dinero").header("Authorization", "Bearer " + master)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().is(esperado));
    }

    private void pagar(String codigo, String cuerpo, int esperado) throws Exception {
        mvc.perform(post("/api/yo/pagos").header("Authorization", "Bearer " + codigo)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().is(esperado));
    }

    // ---------- utilidades ----------

    private JsonNode enviarYLeer(MockHttpServletRequestBuilder peticion, String codigo, String cuerpo) throws Exception {
        String respuesta = mvc.perform(peticion.header("Authorization", "Bearer " + codigo)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return json.readTree(respuesta);
    }

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

    /** Objetivo con ese id dentro del panel de un jugador. */
    private static JsonNode objetivo(JsonNode panel, long objetivoId) {
        for (JsonNode objetivo : panel.get("objetivos")) {
            if (objetivo.get("id").asLong() == objetivoId) {
                return objetivo;
            }
        }
        throw new AssertionError("Objetivo no encontrado: " + objetivoId);
    }

    /** Objetivo de un jugador dentro del estado del Máster. */
    private static JsonNode objetivoDe(JsonNode estado, long jugadorId, long objetivoId) {
        for (JsonNode jugador : estado.get("jugadores")) {
            if (jugador.get("id").asLong() == jugadorId) {
                return objetivo(jugador, objetivoId);
            }
        }
        throw new AssertionError("Jugador no encontrado: " + jugadorId);
    }
}
