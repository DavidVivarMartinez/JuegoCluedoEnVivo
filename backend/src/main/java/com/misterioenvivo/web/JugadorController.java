package com.misterioenvivo.web;

import com.misterioenvivo.seguridad.Sesion;
import com.misterioenvivo.servicio.JuegoService;
import com.misterioenvivo.web.Dtos.PanelJugador;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/yo")
public class JugadorController {

    private final JuegoService juego;

    public JugadorController(JuegoService juego) {
        this.juego = juego;
    }

    @GetMapping
    public PanelJugador panel(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion) {
        return juego.panel(sesion.jugadorId());
    }

    @PostMapping("/mensajes/{id}/leido")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leido(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @PathVariable("id") Long id) {
        juego.marcarLeido(sesion.jugadorId(), id);
    }
}
