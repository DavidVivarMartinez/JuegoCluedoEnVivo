package com.misterioenvivo.web;

import com.misterioenvivo.servicio.JuegoService;
import com.misterioenvivo.web.Dtos.Acceso;
import com.misterioenvivo.web.Dtos.SesionInfo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Entrada a la app: valida un código y dice quién eres y qué panel te toca. */
@RestController
@RequestMapping("/api/sesion")
public class SesionController {

    private final JuegoService juego;

    public SesionController(JuegoService juego) {
        this.juego = juego;
    }

    @PostMapping
    public SesionInfo entrar(@RequestBody Acceso acceso) {
        return juego.sesion(acceso.codigo());
    }
}
