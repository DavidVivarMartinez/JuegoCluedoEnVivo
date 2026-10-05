package com.misterioenvivo.web;

import com.misterioenvivo.seguridad.Sesion;
import com.misterioenvivo.servicio.JuegoService;
import com.misterioenvivo.servicio.MecanicasService;
import com.misterioenvivo.web.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Control manual de la partida. Todo queda acotado a la partida del Máster que llama. */
@RestController
@RequestMapping("/api/master")
public class MasterController {

    private final JuegoService juego;
    private final MecanicasService mecanicas;

    public MasterController(JuegoService juego, MecanicasService mecanicas) {
        this.juego = juego;
        this.mecanicas = mecanicas;
    }

    @GetMapping("/estado")
    public EstadoMaster estado(@RequestAttribute(Sesion.ATRIBUTO) Sesion s) {
        return juego.estado(s.partidaId());
    }

    @PutMapping("/fase")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void fase(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @RequestBody CambioFase datos) {
        juego.cambiarFase(s.partidaId(), datos.fase());
    }

    @PutMapping("/asesino")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void asesino(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @RequestBody CambioAsesino datos) {
        juego.cambiarAsesino(s.partidaId(), datos.jugadorId());
    }

    /** Marca o desmarca a un jugador como asesino (puede haber varios). */
    @PutMapping("/jugadores/{id}/asesino")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarAsesino(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                              @RequestBody MarcaAsesino datos) {
        juego.marcarAsesino(s.partidaId(), id, Boolean.TRUE.equals(datos.asesino()));
    }

    /** Crea o actualiza la pista "El arma" con las armas elegidas por los asesinos (queda bloqueada). */
    @PostMapping("/arma/pista")
    public Map<String, Long> pistaArma(@RequestAttribute(Sesion.ATRIBUTO) Sesion s) {
        return Map.of("id", juego.generarPistaArma(s.partidaId()));
    }

    /** Da (positivo) o quita (negativo) dinero; sin jugadorIds, a todos los jugadores. */
    @PostMapping("/dinero")
    public Map<String, Integer> dinero(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @RequestBody AjusteDinero datos) {
        return Map.of("jugadores", juego.ajustarDinero(s.partidaId(), datos));
    }

    /** Ficha pública del propio Máster: también aparece como personaje en el carrusel de los jugadores. */
    @PutMapping("/ficha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ficha(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @RequestBody FichaEntrada ficha) {
        juego.editarFichaMaster(s.partidaId(), s.jugadorId(), ficha);
    }

    // ---------- Jugadores ----------

    @PostMapping("/jugadores")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> crearJugador(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @RequestBody FichaEntrada ficha) {
        return Map.of("id", juego.crearJugador(s.partidaId(), ficha));
    }

    @PutMapping("/jugadores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editarJugador(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                              @RequestBody FichaEntrada ficha) {
        juego.editarJugador(s.partidaId(), id, ficha);
    }

    @DeleteMapping("/jugadores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarJugador(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id) {
        juego.borrarJugador(s.partidaId(), id);
    }

    // ---------- Secretos ----------

    @PostMapping("/jugadores/{id}/secretos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> crearSecreto(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                                          @RequestBody SecretoEntrada datos) {
        return Map.of("id", juego.crearSecreto(s.partidaId(), id, datos));
    }

    @PutMapping("/secretos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editarSecreto(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                              @RequestBody SecretoEntrada datos) {
        juego.editarSecreto(s.partidaId(), id, datos);
    }

    @DeleteMapping("/secretos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarSecreto(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id) {
        juego.borrarSecreto(s.partidaId(), id);
    }

    // ---------- Objetivos ----------

    @PostMapping("/jugadores/{id}/objetivos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> crearObjetivo(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                                           @RequestBody ObjetivoEntrada datos) {
        return Map.of("id", juego.crearObjetivo(s.partidaId(), id, datos));
    }

    @PutMapping("/objetivos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editarObjetivo(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                               @RequestBody ObjetivoEntrada datos) {
        juego.editarObjetivo(s.partidaId(), id, datos);
    }

    @DeleteMapping("/objetivos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarObjetivo(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id) {
        juego.borrarObjetivo(s.partidaId(), id);
    }

    /** Pista que se desbloquea al jugador cuando el objetivo se da por cumplido (pistaId null = ninguna). */
    @PutMapping("/objetivos/{id}/recompensa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recompensaObjetivo(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                                   @RequestBody RecompensaEntrada datos) {
        juego.recompensaObjetivo(s.partidaId(), id, datos.pistaId());
    }

    // ---------- Pistas ----------

    @PostMapping("/pistas")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> crearPista(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @RequestBody PistaEntrada datos) {
        return Map.of("id", juego.crearPista(s.partidaId(), datos));
    }

    @PutMapping("/pistas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editarPista(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                            @RequestBody PistaEntrada datos) {
        juego.editarPista(s.partidaId(), id, datos);
    }

    @PutMapping("/pistas/{id}/visibilidad")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void visibilidadPista(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                                 @RequestBody VisibilidadPista datos) {
        juego.visibilidadPista(s.partidaId(), id, datos);
    }

    /** Precio en la tienda (null = no está a la venta). */
    @PutMapping("/pistas/{id}/precio")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void precioPista(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                            @RequestBody PrecioPista datos) {
        juego.precioPista(s.partidaId(), id, datos.precio());
    }

    @DeleteMapping("/pistas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarPista(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id) {
        juego.borrarPista(s.partidaId(), id);
    }

    // ---------- Mensajes ----------

    @PostMapping("/mensajes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> enviarMensaje(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @RequestBody MensajeEntrada datos) {
        return Map.of("enviados", juego.enviarMensaje(s.partidaId(), datos));
    }

    // ---------- Reglas, tablón, habilidades, pistas falsas, QR y envíos ----------

    /** Estancias de la casa (una por línea) y precio de una pista falsa. */
    @PutMapping("/reglas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reglas(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @RequestBody ReglasEntrada datos) {
        mecanicas.reglas(s.partidaId(), datos);
    }

    /** Aviso del Máster en el tablón público. */
    @PostMapping("/tablon")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> aviso(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @RequestBody TextoEntrada datos) {
        return Map.of("id", mecanicas.aviso(s.partidaId(), s.jugadorId(), datos.texto()));
    }

    @DeleteMapping("/tablon/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarPublicacion(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id) {
        mecanicas.borrarPublicacion(s.partidaId(), id);
    }

    @PostMapping("/jugadores/{id}/habilidades")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> crearHabilidad(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                                            @RequestBody HabilidadEntrada datos) {
        return Map.of("id", mecanicas.crearHabilidad(s.partidaId(), id, datos));
    }

    @PutMapping("/habilidades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editarHabilidad(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                                @RequestBody HabilidadEntrada datos) {
        mecanicas.editarHabilidad(s.partidaId(), id, datos);
    }

    /** Contesta una habilidad pendiente o la repone (estado DISPONIBLE). */
    @PostMapping("/habilidades/{id}/resolver")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolverHabilidad(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                                  @RequestBody ResolverHabilidad datos) {
        mecanicas.resolverHabilidad(s.partidaId(), id, datos);
    }

    @DeleteMapping("/habilidades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarHabilidad(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id) {
        mecanicas.borrarHabilidad(s.partidaId(), id);
    }

    @PostMapping("/pistas/{id}/falsa/aprobar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aprobarFalsa(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                             @RequestBody AprobarFalsa datos) {
        mecanicas.aprobarFalsa(s.partidaId(), id, datos);
    }

    @PostMapping("/pistas/{id}/falsa/rechazar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rechazarFalsa(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id) {
        mecanicas.rechazarFalsa(s.partidaId(), id);
    }

    /** Activa o quita el QR de una pista; devuelve el código. */
    @PutMapping("/pistas/{id}/qr")
    public Map<String, String> qr(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                                  @RequestBody QrEntrada datos) {
        String codigo = mecanicas.qr(s.partidaId(), id, Boolean.TRUE.equals(datos.activo()));
        return codigo == null ? Map.of() : Map.of("codigo", codigo);
    }

    @PostMapping("/envios")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> crearEnvio(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @RequestBody EnvioEntrada datos) {
        return Map.of("id", mecanicas.crearEnvio(s.partidaId(), datos));
    }

    @PutMapping("/envios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editarEnvio(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                            @RequestBody EnvioEntrada datos) {
        mecanicas.editarEnvio(s.partidaId(), id, datos);
    }

    @DeleteMapping("/envios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarEnvio(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id) {
        mecanicas.borrarEnvio(s.partidaId(), id);
    }

    /** Entrega ya (a sus destinatarios, o una copia a los jugadores indicados). */
    @PostMapping("/envios/{id}/enviar")
    public Map<String, Integer> enviarAhora(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("id") Long id,
                                            @RequestBody(required = false) EnviarAhora datos) {
        return Map.of("jugadores", mecanicas.enviarAhora(s.partidaId(), id, datos));
    }

    // ---------- Variables narrativas ----------

    @PutMapping("/variables/{clave}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void guardarVariable(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("clave") String clave,
                                @RequestBody ValorEntrada datos) {
        juego.guardarVariable(s.partidaId(), clave, datos.valor());
    }

    @DeleteMapping("/variables/{clave}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarVariable(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @PathVariable("clave") String clave) {
        juego.borrarVariable(s.partidaId(), clave);
    }
}
