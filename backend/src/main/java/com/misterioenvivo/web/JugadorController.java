package com.misterioenvivo.web;

import com.misterioenvivo.seguridad.Sesion;
import com.misterioenvivo.servicio.JuegoService;
import com.misterioenvivo.servicio.MecanicasService;
import com.misterioenvivo.web.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/yo")
public class JugadorController {

    private final JuegoService juego;
    private final MecanicasService mecanicas;

    public JugadorController(JuegoService juego, MecanicasService mecanicas) {
        this.juego = juego;
        this.mecanicas = mecanicas;
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

    /** Guarda (o vacía) mi cuaderno privado sobre otro personaje de la partida. */
    @PutMapping("/anotaciones/{jugadorId}")
    public AnotacionDto anotar(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @PathVariable("jugadorId") Long jugadorId,
                               @RequestBody TextoEntrada datos) {
        return juego.guardarAnotacion(sesion.jugadorId(), jugadorId, datos.texto());
    }

    /** Aporta una prueba a un objetivo propio (personaje elegido y/o texto). */
    @PostMapping("/objetivos/{id}/entregas")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> entregar(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @PathVariable("id") Long id,
                                      @RequestBody EntregaEntrada datos) {
        return Map.of("id", juego.entregar(sesion.jugadorId(), id, datos));
    }

    /** Pago a otro participante de la partida. */
    @PostMapping("/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    public TransaccionDto pagar(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @RequestBody PagoEntrada datos) {
        return juego.pagar(sesion.jugadorId(), datos);
    }

    /** Compra una pista de la tienda del Máster. */
    @PostMapping("/tienda/{pistaId}")
    public PistaJugador comprar(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @PathVariable("pistaId") Long pistaId) {
        return juego.comprarPista(sesion.jugadorId(), pistaId);
    }

    /** Solo el asesino: elige de quién es la herramienta que usa (null = ninguna). */
    @PutMapping("/arma")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void arma(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @RequestBody EleccionArma datos) {
        juego.elegirArma(sesion.jugadorId(), datos.jugadorId());
    }

    /** Casilla del cuaderno de deducción (sospechoso, arma o estancia). */
    @PutMapping("/deduccion")
    public MarcaDto marcar(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @RequestBody MarcaEntrada datos) {
        return mecanicas.marcar(sesion.jugadorId(), datos);
    }

    /** Acusación final (quién, con qué y por qué). Vacía = la retira. */
    @PutMapping("/acusacion-final")
    public AcusacionFinalDto acusacionFinal(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion,
                                            @RequestBody AcusacionFinalEntrada datos) {
        return mecanicas.acusacionFinal(sesion.jugadorId(), datos);
    }

    /** Acusación pública en el tablón. */
    @PostMapping("/tablon")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> acusar(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @RequestBody AcusacionEntrada datos) {
        return Map.of("id", mecanicas.acusar(sesion.jugadorId(), datos));
    }

    /** El acusado responde (y puede enseñar una pista). */
    @PostMapping("/tablon/{id}/respuesta")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void responder(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @PathVariable("id") Long id,
                          @RequestBody RespuestaEntrada datos) {
        mecanicas.responder(sesion.jugadorId(), id, datos);
    }

    /** Código de un QR escondido en la casa. */
    @PostMapping("/qr/{codigo}")
    public PistaJugador qr(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @PathVariable("codigo") String codigo) {
        return mecanicas.canjearQr(sesion.jugadorId(), codigo);
    }

    @PostMapping("/habilidades/{id}")
    public HabilidadDto usarHabilidad(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @PathVariable("id") Long id,
                                      @RequestBody UsoHabilidad datos) {
        return mecanicas.usarHabilidad(sesion.jugadorId(), id, datos);
    }

    @PostMapping("/tratos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> proponerTrato(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @RequestBody TratoEntrada datos) {
        return Map.of("id", mecanicas.proponerTrato(sesion.jugadorId(), datos));
    }

    @PostMapping("/tratos/{id}/aceptar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aceptarTrato(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @PathVariable("id") Long id,
                             @RequestBody(required = false) AceptarTrato datos) {
        mecanicas.aceptarTrato(sesion.jugadorId(), id, datos);
    }

    @PostMapping("/tratos/{id}/rechazar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rechazarTrato(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @PathVariable("id") Long id) {
        mecanicas.rechazarTrato(sesion.jugadorId(), id);
    }

    @PostMapping("/tratos/{id}/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarTrato(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @PathVariable("id") Long id) {
        mecanicas.cancelarTrato(sesion.jugadorId(), id);
    }

    /** Solo el asesino: encarga una pista falsa (la paga y el Máster la aprueba). */
    @PostMapping("/pistas-falsas")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> pistaFalsa(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @RequestBody PistaFalsaEntrada datos) {
        return Map.of("id", mecanicas.proponerFalsa(sesion.jugadorId(), datos));
    }

    /** Retira una prueba propia mientras el objetivo siga abierto. */
    @DeleteMapping("/entregas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarEntrega(@RequestAttribute(Sesion.ATRIBUTO) Sesion sesion, @PathVariable("id") Long id) {
        juego.borrarEntrega(sesion.jugadorId(), id);
    }
}
