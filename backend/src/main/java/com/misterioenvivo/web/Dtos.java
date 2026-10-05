package com.misterioenvivo.web;

import com.misterioenvivo.modelo.EstadoFalsa;
import com.misterioenvivo.modelo.EstadoHabilidad;
import com.misterioenvivo.modelo.EstadoObjetivo;
import com.misterioenvivo.modelo.EstadoTrato;
import com.misterioenvivo.modelo.Fase;
import com.misterioenvivo.modelo.Rol;
import com.misterioenvivo.modelo.TipoEnvio;
import com.misterioenvivo.modelo.TipoHabilidad;
import com.misterioenvivo.modelo.TipoObjetivo;
import com.misterioenvivo.modelo.TipoPublicacion;

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
    /** Compatibilidad: deja a ese jugador como único asesino (null = ninguno). */
    public record CambioAsesino(Long jugadorId) { }
    /** Marca o desmarca a un jugador como asesino (puede haber varios). */
    public record MarcaAsesino(Boolean asesino) { }
    /** El asesino elige de quién es la herramienta que usa (la suya o la de otro); null = ninguna. */
    public record EleccionArma(Long jugadorId) { }
    public record FichaEntrada(String nombre, Integer edad, String profesion, String pareja, String imagenUrl,
                               String herramienta, String personalidad, String relaciones, String contexto,
                               String loQueSabeMaster, String motivoPotencial) { }
    public record TextoEntrada(String texto) { }
    public record SecretoEntrada(String texto, Boolean revelado) { }
    public record ObjetivoEntrada(String texto, EstadoObjetivo estado, Boolean principal, TipoObjetivo tipo,
                                  Integer cantidad, Integer recompensaDinero) { }
    /** Pista que se desbloquea al cumplir un objetivo; null la quita. */
    public record RecompensaEntrada(Long pistaId) { }
    /** Prueba que aporta el jugador: personaje elegido (tipo PERSONA) y/o texto. */
    public record EntregaEntrada(Long personaId, String texto) { }
    /** Pista; "nota" es solo para el Máster (null = no se cambia, vacía = se borra). */
    public record PistaEntrada(String titulo, String contenido, String nota) { }
    public record VisibilidadPista(List<Long> jugadorIds) { }
    /** Precio en la tienda; null = retirarla de la venta. */
    public record PrecioPista(Integer precio) { }
    public record MensajeEntrada(String texto, List<Long> jugadorIds) { }
    public record ValorEntrada(String valor) { }
    /** Pago de un jugador a otro participante. */
    public record PagoEntrada(Long jugadorId, Integer cantidad, String concepto) { }
    /** El Máster da (positivo) o quita (negativo) dinero. Sin jugadores = a todos. */
    public record AjusteDinero(Integer cantidad, String concepto, List<Long> jugadorIds) { }
    /** Reglas de la partida: estancias de la casa (una por línea) y precio de una pista falsa. Null = no cambia. */
    public record ReglasEntrada(String lugares, Integer precioPistaFalsa) { }
    /** Casilla del cuaderno de deducción. marca SI / NO / DUDA; null la deja en blanco. */
    public record MarcaEntrada(String categoria, String clave, String marca) { }
    /** Acusación final: quién lo hizo, con la herramienta de quién y por qué. */
    public record AcusacionFinalEntrada(Long sospechosoId, Long armaDeId, String razon) { }
    /** Acusación pública en el tablón. */
    public record AcusacionEntrada(Long acusadoId, String texto) { }
    /** Respuesta del acusado; puede enseñar al grupo una de sus pistas. */
    public record RespuestaEntrada(String texto, Long pistaId) { }
    /** Uso de una habilidad: personaje elegido y/o texto, según lo que pida. */
    public record UsoHabilidad(Long personaId, String texto) { }
    public record HabilidadEntrada(String nombre, String descripcion, TipoHabilidad tipo, Boolean pidePersona, String pideTexto) { }
    /** El Máster contesta una habilidad (la da por resuelta) o la repone (estado DISPONIBLE). */
    public record ResolverHabilidad(String respuesta, EstadoHabilidad estado) { }
    /** Propuesta de trato: lo que ofrezco (pista y/o dinero) y lo que pido (una pista suya y/o dinero). */
    public record TratoEntrada(Long paraId, Long pistaId, Integer dinero, Boolean pidePista, Integer dineroPedido, String mensaje) { }
    /** Al aceptar un trato que pide pista, la que se da a cambio. */
    public record AceptarTrato(Long pistaId) { }
    /** Pista falsa que propone un asesino; paraId null = a la tienda. */
    public record PistaFalsaEntrada(String titulo, String contenido, Long paraId) { }
    /** El Máster aprueba una pista falsa, pudiendo retocarla; precio solo si va a la tienda. */
    public record AprobarFalsa(String titulo, String contenido, Integer precio) { }
    /** Sobre o visión. Sin hora = solo a mano; sin destinatarios ni hora = visión de reserva. */
    public record EnvioEntrada(TipoEnvio tipo, Long pistaId, String imagenUrl, String nota, List<Long> jugadorIds,
                               Instant programadaPara, Boolean anunciar) { }
    /** Entregar ya: un programado a sus destinatarios, o una visión de reserva a los indicados. */
    public record EnviarAhora(List<Long> jugadorIds) { }
    public record QrEntrada(Boolean activo) { }

    // ---------- Salida común ----------
    public record PartidaInfo(Long id, String nombre, String casa, LocalDate fechaInicio, LocalDate fechaFin, Fase fase) { }
    public record SesionInfo(Long jugadorId, String nombre, Rol rol, PartidaInfo partida) { }
    public record SecretoDto(Long id, String texto, boolean revelado) { }
    public record EntregaDto(Long id, Long personaId, String persona, String texto, Instant creadoEn) { }
    public record MensajeDto(Long id, String texto, Instant enviadoEn, boolean leido) { }
    /** Movimiento de dinero; de/para null = la banca (el Máster). */
    public record TransaccionDto(Long id, Long deId, String de, Long paraId, String para, int cantidad,
                                 String concepto, Instant creadoEn) { }
    /** Lo que cualquier participante puede ver de un personaje (también del Máster). */
    public record FichaPublica(Long id, String nombre, Integer edad, String profesion, String pareja, String imagenUrl) { }

    public record PistaTitulo(Long id, String titulo) { }
    public record MarcaDto(String categoria, String clave, String marca) { }
    /** Entrada del tablón. En un rumor el autor llega vacío a los jugadores. */
    public record PublicacionDto(Long id, TipoPublicacion tipo, Long autorId, String autor, Long acusadoId, String acusado,
                                 String texto, String respuesta, PistaJugador pistaMostrada, Instant creadaEn,
                                 Instant respondidaEn) { }
    public record HabilidadDto(Long id, String nombre, String descripcion, TipoHabilidad tipo, boolean pidePersona,
                               String pideTexto, EstadoHabilidad estado, Long personaId, String persona, String peticion,
                               String respuesta, Instant usadaEn) { }
    public record AcusacionFinalDto(Long jugadorId, String jugador, Long sospechosoId, String sospechoso, Long armaDeId,
                                    String arma, String razon, Instant en) { }

    // ---------- Admin ----------
    public record PartidaCreada(PartidaInfo partida, String codigoMaster) { }

    // ---------- Panel del jugador ----------
    public record Personaje(Long id, String nombre, Integer edad, String profesion, String pareja, String imagenUrl,
                            String herramienta, String personalidad, String relaciones, String contexto) { }
    public record SecretoRevelado(String jugador, String texto) { }
    public record PistaJugador(Long id, String titulo, String contenido) { }
    /** Pista a la venta: se ve el título y el precio, nunca el contenido hasta comprarla. */
    public record PistaEnVenta(Long id, String titulo, int precio) { }
    /** Solo lo recibe un asesino: sus cómplices, el arma que ha elegido y sus pistas falsas. */
    public record Asesinato(List<String> complices, Long armaDeId, boolean armaBloqueada, int precioPistaFalsa,
                            List<PistaFalsaDto> pistasFalsas) { }
    public record PistaFalsaDto(Long id, String titulo, String contenido, String para, EstadoFalsa estado) { }
    /** Imagen que manda la víctima. Sin texto: el jugador la interpreta. */
    public record VisionDto(Long id, String imagenUrl, Instant recibidaEn) { }
    /** Sobre programado que todavía no se ha abierto: solo se sabe cuándo. */
    public record SobreSellado(Long id, Instant abreEn) { }
    /** Trato visto por uno de los dos jugadores; "puedoDar" son las pistas mías que el otro no tiene (si me piden una). */
    public record TratoDto(Long id, Long deId, String de, Long paraId, String para, PistaTitulo pistaOfrecida,
                           int dineroOfrecido, boolean pidePista, int dineroPedido, String mensaje,
                           PistaTitulo pistaRecibida, EstadoTrato estado, Instant creadoEn, List<PistaTitulo> puedoDar) { }
    /** Objetivo tal y como lo ve su dueño: sabe si hay premio, pero no cuál. */
    public record ObjetivoDto(Long id, String texto, EstadoObjetivo estado, boolean principal, TipoObjetivo tipo,
                              int cantidad, boolean tieneRecompensa, int recompensaDinero, List<EntregaDto> entregas) { }
    /** Otro personaje de la partida (el Máster incluido): su ficha pública y lo que yo he anotado sobre él. */
    public record OtroPersonaje(Long id, String nombre, Integer edad, String profesion, String pareja, String imagenUrl,
                                String herramienta, boolean esMaster, String anotacion, Instant anotacionEn) { }
    public record AnotacionDto(Long jugadorId, String texto, Instant actualizadoEn) { }
    public record PanelJugador(PartidaInfo partida, Personaje personaje, boolean esAsesino, Asesinato asesinato,
                               List<SecretoDto> secretos, List<SecretoRevelado> secretosRevelados,
                               List<PistaJugador> pistas, List<ObjetivoDto> objetivos,
                               List<MensajeDto> mensajes, List<OtroPersonaje> otrosPersonajes,
                               int dinero, List<TransaccionDto> movimientos, List<PistaEnVenta> tienda,
                               List<String> lugares, List<MarcaDto> deduccion, List<PublicacionDto> tablon,
                               List<VisionDto> visiones, List<SobreSellado> sobresSellados, List<HabilidadDto> habilidades,
                               List<TratoDto> tratos, AcusacionFinalDto acusacionFinal,
                               List<AcusacionFinalDto> acusacionesFinales) { }

    // ---------- Panel del Máster ----------
    public record ObjetivoMaster(Long id, String texto, EstadoObjetivo estado, boolean principal, TipoObjetivo tipo,
                                 int cantidad, Long recompensaPistaId, int recompensaDinero, List<EntregaDto> entregas) { }
    public record JugadorMaster(Long id, String nombre, String codigoAcceso, Integer edad, String profesion,
                                String pareja, String imagenUrl, String herramienta, String personalidad,
                                String relaciones, String contexto, String loQueSabeMaster, String motivoPotencial,
                                boolean asesino, Long armaDeId, int dinero,
                                List<SecretoDto> secretos, List<ObjetivoMaster> objetivos, long mensajesSinLeer,
                                List<HabilidadDto> habilidades) { }
    public record PistaMaster(Long id, String titulo, String contenido, String nota, Integer precio, List<Long> jugadorIds,
                              String codigoQr, boolean falsa, EstadoFalsa estadoFalsa, Long autorFalsaId, Long falsaParaId) { }
    public record EnvioMaster(Long id, TipoEnvio tipo, Long pistaId, String pistaTitulo, String imagenUrl, String nota,
                              List<Long> jugadorIds, Instant programadaPara, Instant enviadoEn, boolean anunciar) { }
    /** Marcador de un jugador. aciertaAsesino/aciertaArma son null si aún no ha hecho su acusación final. */
    public record RankingDto(Long jugadorId, String nombre, boolean asesino, int puntos, int objetivosCumplidos,
                             int objetivosTotal, boolean principalCumplido, int pistas, int pistasCompradas, int dinero,
                             AcusacionFinalDto acusacionFinal, Boolean aciertaAsesino, Boolean aciertaArma,
                             List<String> desglose) { }
    public record VariableDto(String clave, String valor) { }
    public record EventoDto(Long id, String tipo, String descripcion, Instant creadoEn) { }
    public record MensajeMaster(Long id, Long jugadorId, String jugador, String texto, Instant enviadoEn, boolean leido) { }
    public record EstadoMaster(PartidaInfo partida, FichaPublica master, List<Long> asesinoIds, List<JugadorMaster> jugadores,
                               List<PistaMaster> pistas, List<VariableDto> variables,
                               List<MensajeMaster> mensajes, List<EventoDto> eventos, List<TransaccionDto> transacciones,
                               String lugares, int precioPistaFalsa, List<PublicacionDto> tablon, List<EnvioMaster> envios,
                               List<RankingDto> ranking) { }
}
