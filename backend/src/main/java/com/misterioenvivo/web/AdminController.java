package com.misterioenvivo.web;

import com.misterioenvivo.servicio.JuegoService;
import com.misterioenvivo.web.Dtos.NuevaPartida;
import com.misterioenvivo.web.Dtos.PartidaCreada;
import com.misterioenvivo.web.Dtos.PartidaInfo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestión de la plataforma: una partida por grupo/casa. Requiere X-Admin-Key. */
@RestController
@RequestMapping("/api/admin/partidas")
public class AdminController {

    private final JuegoService juego;

    public AdminController(JuegoService juego) {
        this.juego = juego;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartidaCreada crear(@RequestBody NuevaPartida datos) {
        return juego.crearPartida(datos);
    }

    @GetMapping
    public List<PartidaInfo> listar() {
        return juego.listarPartidas();
    }
}
