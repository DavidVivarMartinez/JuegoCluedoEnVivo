package com.misterioenvivo.web;

import com.misterioenvivo.modelo.EstadoObjetivo;
import com.misterioenvivo.modelo.Fase;
import com.misterioenvivo.modelo.Rol;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Objetos de entrada y salida de la API. Las entidades nunca salen directamente. */
public final class Dtos {
    private Dtos() { }

    // ---------- Entrada ----------
    public record NuevaPartida(String nombre, String casa, LocalDate fechaInicio, LocalDate fechaFin, String nombreMaster) { }
    public record Acceso(String codigo) { }
    public record CambioFase(Fase fase) { }
    public record CambioAsesino(Long jugadorId) { }
    public record FichaEntrada(String nombre, Integer edad, String profesion, String pareja, String personalidad,
                               String relaciones, String contexto, String loQueSabeMaster, String motivoPotencial) { }
    public record TextoEntrada(String texto) { }
    public record SecretoEntrada(String texto, Boolean revelado) { }
    public record ObjetivoEntrada(String texto, EstadoObjetivo estado) { }
    public record PistaEntrada(String titulo, String contenido) { }
    public record VisibilidadPista(List<Long> jugadorIds) { }
    public record MensajeEntrada(String texto, List<Long> jugadorIds) { }
    public record ValorEntrada(String valor) { }

    // ---------- Salida común ----------
    public record PartidaInfo(Long id, String nombre, String casa, LocalDate fechaInicio, LocalDate fechaFin, Fase fase) { }
    public record SesionInfo(Long jugadorId, String nombre, Rol rol, PartidaInfo partida) { }
    public record SecretoDto(Long id, String texto, boolean revelado) { }
    public record ObjetivoDto(Long id, String texto, EstadoObjetivo estado) { }
    public record MensajeDto(Long id, String texto, Instant enviadoEn, boolean leido) { }

    // ---------- Admin ----------
    public record PartidaCreada(PartidaInfo partida, String codigoMaster) { }

    // ---------- Panel del jugador ----------
    public record Personaje(String nombre, Integer edad, String profesion, String pareja, String personalidad,
                            String relaciones, String contexto) { }
    public record SecretoRevelado(String jugador, String texto) { }
    public record PistaJugador(Long id, String titulo, String contenido) { }
    public record PanelJugador(PartidaInfo partida, Personaje personaje, boolean esAsesino,
                               List<SecretoDto> secretos, List<SecretoRevelado> secretosRevelados,
                               List<PistaJugador> pistas, List<ObjetivoDto> objetivos,
                               List<MensajeDto> mensajes, List<String> participantes) { }

    // ---------- Panel del Máster ----------
    public record JugadorMaster(Long id, String nombre, String codigoAcceso, Integer edad, String profesion,
                                String pareja, String personalidad, String relaciones, String contexto,
                                String loQueSabeMaster, String motivoPotencial,
                                List<SecretoDto> secretos, List<ObjetivoDto> objetivos, long mensajesSinLeer) { }
    public record PistaMaster(Long id, String titulo, String contenido, List<Long> jugadorIds) { }
    public record VariableDto(String clave, String valor) { }
    public record EventoDto(Long id, String tipo, String descripcion, Instant creadoEn) { }
    public record MensajeMaster(Long id, Long jugadorId, String jugador, String texto, Instant enviadoEn, boolean leido) { }
    public record EstadoMaster(PartidaInfo partida, Long asesinoId, List<JugadorMaster> jugadores,
                               List<PistaMaster> pistas, List<VariableDto> variables,
                               List<MensajeMaster> mensajes, List<EventoDto> eventos) { }
}
