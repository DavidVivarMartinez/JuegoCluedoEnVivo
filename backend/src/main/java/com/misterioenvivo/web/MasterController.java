package com.misterioenvivo.web;

import com.misterioenvivo.seguridad.Sesion;
import com.misterioenvivo.servicio.JuegoService;
import com.misterioenvivo.web.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Control manual de la partida. Todo queda acotado a la partida del Máster que llama. */
@RestController
@RequestMapping("/api/master")
public class MasterController {

    private final JuegoService juego;

    public MasterController(JuegoService juego) {
        this.juego = juego;
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
