package com.misterioenvivo.servicio;

import com.misterioenvivo.modelo.*;
import com.misterioenvivo.repo.*;
import com.misterioenvivo.seguridad.GeneradorCodigos;
import com.misterioenvivo.web.Dtos.*;
import com.misterioenvivo.web.ImagenController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Autoridad del estado de la partida. No contiene historia: todo el contenido
 * narrativo (fichas, secretos, pistas, asesino) son datos que introduce el Máster.
 */
@Service
@Transactional
public class JuegoService {

    private final PartidaRepository partidas;
    private final JugadorRepository jugadores;
    private final SecretoRepository secretos;
    private final ObjetivoRepository objetivos;
    private final EntregaRepository entregas;
    private final PistaRepository pistas;
    private final MensajeRepository mensajes;
    private final EventoRepository eventos;
    private final VariableRepository variables;
    private final AnotacionRepository anotaciones;
    private final TransaccionRepository transacciones;
    private final MarcaDeduccionRepository marcas;
    private final PublicacionRepository publicaciones;
    private final EnvioRepository envios;
    private final HabilidadRepository habilidades;
    private final TratoRepository tratos;
    private final GeneradorCodigos codigos;

    public JuegoService(PartidaRepository partidas, JugadorRepository jugadores, SecretoRepository secretos,
                        ObjetivoRepository objetivos, EntregaRepository entregas, PistaRepository pistas,
                        MensajeRepository mensajes, EventoRepository eventos, VariableRepository variables,
                        AnotacionRepository anotaciones, TransaccionRepository transacciones,
                        MarcaDeduccionRepository marcas, PublicacionRepository publicaciones, EnvioRepository envios,
                        HabilidadRepository habilidades, TratoRepository tratos, GeneradorCodigos codigos) {
        this.partidas = partidas;
        this.jugadores = jugadores;
        this.secretos = secretos;
        this.objetivos = objetivos;
        this.entregas = entregas;
        this.pistas = pistas;
        this.mensajes = mensajes;
        this.eventos = eventos;
        this.variables = variables;
        this.anotaciones = anotaciones;
        this.transacciones = transacciones;
        this.marcas = marcas;
        this.publicaciones = publicaciones;
        this.envios = envios;
        this.habilidades = habilidades;
        this.tratos = tratos;
        this.codigos = codigos;
    }

    // =====================================================================
    // Administración (plataforma): partidas / casas
    // =====================================================================

    public PartidaCreada crearPartida(NuevaPartida datos) {
        Partida partida = new Partida();
        partida.setNombre(requerido(datos.nombre(), "nombre"));
        partida.setCasa(limpio(datos.casa()));
        partida.setFechaInicio(datos.fechaInicio());
        partida.setFechaFin(datos.fechaFin());
        partidas.save(partida);

        Jugador master = new Jugador();
        master.setPartida(partida);
        master.setNombre(requerido(datos.nombreMaster(), "nombreMaster"));
        master.setRol(Rol.MASTER);
        master.setCodigoAcceso(codigos.nuevo());
        jugadores.save(master);

        registrar(partida, "PARTIDA", "Partida creada");
        return new PartidaCreada(info(partida), master.getCodigoAcceso());
    }

    @Transactional(readOnly = true)
    public List<PartidaInfo> listarPartidas() {
        return partidas.findAll().stream().map(JuegoService::info).toList();
    }

    // =====================================================================
    // Sesión
    // =====================================================================

    @Transactional(readOnly = true)
    public SesionInfo sesion(String codigo) {
        String normalizado = requerido(codigo, "codigo").toUpperCase();
        Jugador jugador = jugadores.buscarPorCodigo(normalizado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código de acceso no válido"));
        return new SesionInfo(jugador.getId(), jugador.getNombre(), jugador.getRol(), info(jugador.getPartida()));
    }

    // =====================================================================
    // Panel del jugador: solo lo que ese jugador puede saber
    // =====================================================================

    @Transactional(readOnly = true)
    public PanelJugador panel(Long jugadorId) {
        Jugador yo = jugadores.findById(jugadorId)
                .orElseThrow(() -> noEncontrado("Jugador"));
        Partida partida = yo.getPartida();
        Long partidaId = partida.getId();

        boolean esAsesino = yo.isAsesino();
        Asesinato asesinato = !esAsesino ? null : new Asesinato(
                jugadores.findByPartidaIdAndAsesinoTrueOrderByNombreAsc(partidaId).stream()
                        .filter(j -> !j.getId().equals(jugadorId)).map(Jugador::getNombre).toList(),
                yo.getArmaDe() == null ? null : yo.getArmaDe().getId(),
                armaBloqueada(partida),
                partida.getPrecioPistaFalsa(),
                pistas.findByAutorFalsaIdOrderByIdAsc(jugadorId).stream()
                        .map(p -> new PistaFalsaDto(p.getId(), p.getTitulo(), p.getContenido(),
                                p.getFalsaPara() == null ? null : p.getFalsaPara().getNombre(), p.getEstadoFalsa()))
                        .toList());

        List<SecretoRevelado> revelados = secretos.findByJugadorPartidaIdOrderByIdAsc(partidaId).stream()
                .filter(Secreto::isRevelado)
                .map(s -> new SecretoRevelado(s.getJugador().getNombre(), s.getTexto()))
                .toList();

        // Los demás personajes (el Máster también lo es): ficha pública más lo que yo haya anotado de cada uno.
        Map<Long, Anotacion> misAnotaciones = anotaciones.findByAutorId(jugadorId).stream()
                .collect(Collectors.toMap(a -> a.getSobre().getId(), a -> a));
        List<OtroPersonaje> otros = jugadores.findByPartidaIdOrderByNombreAsc(partidaId).stream()
                .filter(j -> !j.getId().equals(jugadorId))
                .map(j -> {
                    Anotacion nota = misAnotaciones.get(j.getId());
                    return new OtroPersonaje(j.getId(), j.getNombre(), j.getEdad(), j.getProfesion(), j.getPareja(),
                            j.getImagenUrl(), j.getHerramienta(), j.getRol() == Rol.MASTER,
                            nota == null ? null : nota.getTexto(),
                            nota == null ? null : nota.getActualizadoEn());
                })
                .toList();

        Map<Long, List<EntregaDto>> entregasPorObjetivo = agrupar(entregas.findByObjetivoJugadorIdOrderByIdAsc(jugadorId));
        List<ObjetivoDto> misObjetivos = objetivos.findByJugadorIdOrderByIdAsc(jugadorId).stream()
                .map(o -> new ObjetivoDto(o.getId(), o.getTexto(), o.getEstado(), o.isPrincipal(), o.getTipo(),
                        o.getCantidad(), o.getRecompensa() != null, o.getRecompensaDinero(),
                        entregasPorObjetivo.getOrDefault(o.getId(), List.of())))
                .toList();

        List<Pista> visibles = pistas.visiblesPara(jugadorId);
        java.util.Set<Long> yaTengo = visibles.stream().map(Pista::getId).collect(Collectors.toSet());
        List<PistaEnVenta> tienda = pistas.findByPartidaIdOrderByIdAsc(partidaId).stream()
                .filter(p -> p.getPrecio() != null && !yaTengo.contains(p.getId()))
                .map(p -> new PistaEnVenta(p.getId(), p.getTitulo(), p.getPrecio()))
                .toList();

        // Lo que la víctima y el Máster le han hecho llegar: visiones ya recibidas y sobres que aún no se abren.
        List<Envio> misEnvios = envios.dirigidosA(jugadorId);
        List<VisionDto> visiones = misEnvios.stream()
                .filter(e -> e.getTipo() == TipoEnvio.VISION && e.getEnviadoEn() != null)
                .sorted(Comparator.comparing(Envio::getEnviadoEn).reversed())
                .map(e -> new VisionDto(e.getId(), e.getImagenUrl(), e.getEnviadoEn()))
                .toList();
        List<SobreSellado> sellados = misEnvios.stream()
                .filter(e -> e.getTipo() == TipoEnvio.PISTA && e.getEnviadoEn() == null && e.isAnunciar()
                        && e.getProgramadaPara() != null)
                .sorted(Comparator.comparing(Envio::getProgramadaPara))
                .map(e -> new SobreSellado(e.getId(), e.getProgramadaPara()))
                .toList();

        List<TratoDto> misTratos = tratos.deJugador(jugadorId).stream().limit(30)
                .map(t -> dtoTrato(t, jugadorId, yaTengo))
                .toList();

        // Las acusaciones finales de todos solo se ven cuando la partida ha terminado.
        List<AcusacionFinalDto> finales = partida.getFase() != Fase.FINALIZADA ? List.of()
                : jugadores.findByPartidaIdAndRolOrderByNombreAsc(partidaId, Rol.JUGADOR).stream()
                        .filter(j -> j.getFinalEn() != null).map(JuegoService::dtoFinal).toList();

        return new PanelJugador(
                info(partida),
                new Personaje(yo.getId(), yo.getNombre(), yo.getEdad(), yo.getProfesion(), yo.getPareja(), yo.getImagenUrl(),
                        yo.getHerramienta(), yo.getPersonalidad(), yo.getRelaciones(), yo.getContexto()),
                esAsesino,
                asesinato,
                secretos.findByJugadorIdOrderByIdAsc(jugadorId).stream().map(JuegoService::dtoSecreto).toList(),
                revelados,
                visibles.stream().map(p -> new PistaJugador(p.getId(), p.getTitulo(), p.getContenido())).toList(),
                misObjetivos,
                mensajes.findByDestinatarioIdOrderByIdDesc(jugadorId).stream().map(JuegoService::dtoMensaje).toList(),
                otros,
                yo.getDinero(),
                transacciones.deJugador(partidaId, jugadorId).stream().limit(50).map(JuegoService::dtoTransaccion).toList(),
                tienda,
                lineas(partida.getLugares()),
                marcas.findByAutorId(jugadorId).stream()
                        .map(m -> new MarcaDto(m.getCategoria(), m.getClave(), m.getMarca())).toList(),
                publicaciones.findTop100ByPartidaIdOrderByIdDesc(partidaId).stream()
                        .map(p -> dtoPublicacion(p, false)).toList(),
                visiones,
                sellados,
                habilidades.findByJugadorIdOrderByIdAsc(jugadorId).stream().map(JuegoService::dtoHabilidad).toList(),
                misTratos,
                yo.getFinalEn() == null ? null : dtoFinal(yo),
                finales);
    }

    /** Un trato visto por uno de sus dos jugadores. Si me piden una pista, cuáles mías no tiene el otro. */
    private TratoDto dtoTrato(Trato t, Long yoId, java.util.Set<Long> misPistas) {
        List<PistaTitulo> puedoDar = List.of();
        if (t.getEstado() == EstadoTrato.PROPUESTO && t.isPidePista() && t.getPara().getId().equals(yoId)) {
            java.util.Set<Long> suyas = pistas.visiblesPara(t.getDe().getId()).stream()
                    .map(Pista::getId).collect(Collectors.toSet());
            puedoDar = pistas.visiblesPara(yoId).stream()
                    .filter(p -> !suyas.contains(p.getId()))
                    .map(p -> new PistaTitulo(p.getId(), p.getTitulo())).toList();
        }
        return new TratoDto(t.getId(), t.getDe().getId(), t.getDe().getNombre(), t.getPara().getId(), t.getPara().getNombre(),
                t.getPistaOfrecida() == null ? null : new PistaTitulo(t.getPistaOfrecida().getId(), t.getPistaOfrecida().getTitulo()),
                t.getDineroOfrecido(), t.isPidePista(), t.getDineroPedido(), t.getMensaje(),
                t.getPistaRecibida() == null ? null : new PistaTitulo(t.getPistaRecibida().getId(), t.getPistaRecibida().getTitulo()),
                t.getEstado(), t.getCreadoEn(), puedoDar);
    }

    public void marcarLeido(Long jugadorId, Long mensajeId) {
        Mensaje mensaje = mensajes.findByIdAndDestinatarioId(mensajeId, jugadorId)
                .orElseThrow(() -> noEncontrado("Mensaje"));
        mensaje.setLeido(true);
    }

    /**
     * Guarda el cuaderno privado de un jugador sobre otro personaje de su partida.
     * Un texto vacío lo deja en blanco; nunca se comparte con nadie más.
     */
    public AnotacionDto guardarAnotacion(Long jugadorId, Long sobreId, String texto) {
        Jugador yo = jugadores.findById(jugadorId).orElseThrow(() -> noEncontrado("Jugador"));
        if (yo.getId().equals(sobreId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las anotaciones son sobre los demás personajes");
        }
        // Vale cualquier participante de mi partida, el Máster incluido (también es un personaje).
        Jugador sobre = jugadores.findByIdAndPartidaId(sobreId, yo.getPartida().getId())
                .orElseThrow(() -> noEncontrado("Jugador"));
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La anotación es demasiado larga (máximo 4000 caracteres)");
        }
        Anotacion nota = anotaciones.findByAutorIdAndSobreId(jugadorId, sobreId).orElseGet(() -> {
            Anotacion nueva = new Anotacion();
            nueva.setAutor(yo);
            nueva.setSobre(sobre);
            return nueva;
        });
        nota.setTexto(limpio);
        nota.setActualizadoEn(Instant.now());
        anotaciones.save(nota);
        return new AnotacionDto(sobreId, nota.getTexto(), nota.getActualizadoEn());
    }

    // ---------- Entregas: las pruebas con las que el jugador reclama un objetivo ----------

    /** Aporta una prueba a un objetivo propio. Al alcanzar la cantidad necesaria, queda ENTREGADO para el Máster. */
    public Long entregar(Long jugadorId, Long objetivoId, EntregaEntrada datos) {
        Objetivo objetivo = objetivos.findById(objetivoId)
                .filter(o -> o.getJugador().getId().equals(jugadorId))
                .orElseThrow(() -> noEncontrado("Objetivo"));
        exigirAbierto(objetivo);

        String texto = limpio(datos.texto());
        if (texto != null && texto.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El texto es demasiado largo (máximo 2000 caracteres)");
        }

        Entrega entrega = new Entrega();
        entrega.setObjetivo(objetivo);
        switch (objetivo.getTipo()) {
            case LOGRO -> {
                // Un logro se marca una sola vez; volver a enviarlo solo actualiza la nota.
                List<Entrega> previas = entregas.findByObjetivoIdOrderByIdAsc(objetivoId);
                if (!previas.isEmpty()) {
                    entrega = previas.get(0);
                }
                entrega.setTexto(texto);
            }
            case TEXTO -> entrega.setTexto(requerido(datos.texto(), "texto"));
            case PERSONA -> {
                if (datos.personaId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige a un personaje");
                }
                if (datos.personaId().equals(jugadorId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tienes que elegir a otra persona");
                }
                Jugador persona = jugadores.findByIdAndPartidaId(datos.personaId(), objetivo.getJugador().getPartida().getId())
                        .orElseThrow(() -> noEncontrado("Personaje"));
                entrega.setPersona(persona);
                entrega.setTexto(texto);
            }
        }
        entregas.save(entrega);

        if (objetivo.getEstado() == EstadoObjetivo.ACTIVO
                && entregas.countByObjetivoId(objetivoId) >= objetivo.getCantidad()) {
            objetivo.setEstado(EstadoObjetivo.ENTREGADO);
            registrar(objetivo.getJugador().getPartida(), "OBJETIVO",
                    objetivo.getJugador().getNombre() + " ha entregado un objetivo: " + resumen(objetivo.getTexto()));
        }
        return entrega.getId();
    }

    /** Retira una prueba propia mientras el Máster no haya cerrado el objetivo. */
    public void borrarEntrega(Long jugadorId, Long entregaId) {
        Entrega entrega = entregas.findById(entregaId)
                .filter(e -> e.getObjetivo().getJugador().getId().equals(jugadorId))
                .orElseThrow(() -> noEncontrado("Entrega"));
        Objetivo objetivo = entrega.getObjetivo();
        exigirAbierto(objetivo);
        entregas.delete(entrega);
        if (objetivo.getEstado() == EstadoObjetivo.ENTREGADO
                && entregas.countByObjetivoId(objetivo.getId()) < objetivo.getCantidad()) {
            objetivo.setEstado(EstadoObjetivo.ACTIVO);
        }
    }

    private static void exigirAbierto(Objetivo objetivo) {
        if (objetivo.getEstado() == EstadoObjetivo.CUMPLIDO || objetivo.getEstado() == EstadoObjetivo.FALLIDO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El Máster ya ha cerrado este objetivo");
        }
    }

    // =====================================================================
    // Panel del Máster
    // =====================================================================

    @Transactional(readOnly = true)
    public EstadoMaster estado(Long partidaId) {
        Partida partida = partida(partidaId);

        Map<Long, List<SecretoDto>> secretosPorJugador = secretos.findByJugadorPartidaIdOrderByIdAsc(partidaId).stream()
                .collect(Collectors.groupingBy(s -> s.getJugador().getId(),
                        Collectors.mapping(JuegoService::dtoSecreto, Collectors.toList())));
        Map<Long, List<EntregaDto>> entregasPorObjetivo = agrupar(entregas.findByObjetivoJugadorPartidaIdOrderByIdAsc(partidaId));
        Map<Long, List<ObjetivoMaster>> objetivosPorJugador = objetivos.findByJugadorPartidaIdOrderByIdAsc(partidaId).stream()
                .collect(Collectors.groupingBy(o -> o.getJugador().getId(),
                        Collectors.mapping(o -> new ObjetivoMaster(o.getId(), o.getTexto(), o.getEstado(), o.isPrincipal(),
                                o.getTipo(), o.getCantidad(),
                                o.getRecompensa() == null ? null : o.getRecompensa().getId(), o.getRecompensaDinero(),
                                entregasPorObjetivo.getOrDefault(o.getId(), List.of())), Collectors.toList())));
        List<Mensaje> ultimosMensajes = mensajes.findTop100ByDestinatarioPartidaIdOrderByIdDesc(partidaId);
        Map<Long, Long> sinLeer = ultimosMensajes.stream()
                .filter(m -> !m.isLeido())
                .collect(Collectors.groupingBy(m -> m.getDestinatario().getId(), Collectors.counting()));

        Map<Long, List<HabilidadDto>> habilidadesPorJugador = habilidades.findByJugadorPartidaIdOrderByIdAsc(partidaId).stream()
                .collect(Collectors.groupingBy(h -> h.getJugador().getId(),
                        Collectors.mapping(JuegoService::dtoHabilidad, Collectors.toList())));

        List<Jugador> participantes = jugadores.findByPartidaIdAndRolOrderByNombreAsc(partidaId, Rol.JUGADOR);
        List<JugadorMaster> listaJugadores = participantes.stream()
                .map(j -> new JugadorMaster(j.getId(), j.getNombre(), j.getCodigoAcceso(), j.getEdad(),
                        j.getProfesion(), j.getPareja(), j.getImagenUrl(), j.getHerramienta(), j.getPersonalidad(),
                        j.getRelaciones(), j.getContexto(), j.getLoQueSabeMaster(), j.getMotivoPotencial(),
                        j.isAsesino(), j.getArmaDe() == null ? null : j.getArmaDe().getId(), j.getDinero(),
                        secretosPorJugador.getOrDefault(j.getId(), List.of()),
                        objetivosPorJugador.getOrDefault(j.getId(), List.of()),
                        sinLeer.getOrDefault(j.getId(), 0L),
                        habilidadesPorJugador.getOrDefault(j.getId(), List.of())))
                .toList();

        List<PistaMaster> listaPistas = pistas.findByPartidaIdOrderByIdAsc(partidaId).stream()
                .map(p -> new PistaMaster(p.getId(), p.getTitulo(), p.getContenido(), p.getNota(), p.getPrecio(),
                        p.getDescubiertaPor().stream().map(Jugador::getId).sorted().toList(),
                        p.getCodigoQr(), p.isFalsa(), p.getEstadoFalsa(),
                        p.getAutorFalsa() == null ? null : p.getAutorFalsa().getId(),
                        p.getFalsaPara() == null ? null : p.getFalsaPara().getId()))
                .toList();

        List<EnvioMaster> listaEnvios = envios.findByPartidaIdOrderByIdAsc(partidaId).stream()
                .map(e -> new EnvioMaster(e.getId(), e.getTipo(),
                        e.getPista() == null ? null : e.getPista().getId(),
                        e.getPista() == null ? null : e.getPista().getTitulo(),
                        e.getImagenUrl(), e.getNota(),
                        e.getDestinatarios().stream().map(Jugador::getId).sorted().toList(),
                        e.getProgramadaPara(), e.getEnviadoEn(), e.isAnunciar()))
                .toList();

        // El Máster también es un personaje para los jugadores: su ficha pública se edita desde su panel.
        Jugador master = jugadores.findByPartidaIdAndRolOrderByNombreAsc(partidaId, Rol.MASTER).stream()
                .findFirst().orElseThrow(() -> noEncontrado("Máster"));

        return new EstadoMaster(
                info(partida),
                new FichaPublica(master.getId(), master.getNombre(), master.getEdad(), master.getProfesion(),
                        master.getPareja(), master.getImagenUrl()),
                listaJugadores.stream().filter(JugadorMaster::asesino).map(JugadorMaster::id).toList(),
                listaJugadores,
                listaPistas,
                variables.findByPartidaIdOrderByClaveAsc(partidaId).stream()
                        .map(v -> new VariableDto(v.getClave(), v.getValor())).toList(),
                ultimosMensajes.stream()
                        .map(m -> new MensajeMaster(m.getId(), m.getDestinatario().getId(),
                                m.getDestinatario().getNombre(), m.getTexto(), m.getEnviadoEn(), m.isLeido()))
                        .toList(),
                eventos.findTop100ByPartidaIdOrderByIdDesc(partidaId).stream()
                        .map(e -> new EventoDto(e.getId(), e.getTipo(), e.getDescripcion(), e.getCreadoEn()))
                        .toList(),
                transacciones.findTop100ByPartidaIdOrderByIdDesc(partidaId).stream()
                        .map(JuegoService::dtoTransaccion).toList(),
                partida.getLugares(),
                partida.getPrecioPistaFalsa(),
                publicaciones.findTop100ByPartidaIdOrderByIdDesc(partidaId).stream()
                        .map(p -> dtoPublicacion(p, true)).toList(),
                listaEnvios,
                ranking(partidaId, participantes, objetivosPorJugador, listaPistas));
    }

    // ---------- Ranking (solo Máster) ----------

    // Puntos del marcador. Premia acertar y cumplir; cada pista comprada resta, para que no compense vaciar la tienda.
    private static final int PUNTOS_PRINCIPAL = 50;
    private static final int PUNTOS_SECUNDARIO = 20;
    private static final int PUNTOS_ASESINO = 100;
    private static final int PUNTOS_ARMA = 50;
    private static final int PUNTOS_POR_PISTA_COMPRADA = -10;
    /** El asesino gana por cada acusación final que no le señala. */
    private static final int PUNTOS_ASESINO_LIBRE = 15;
    /** Monedas que equivalen a un punto. */
    private static final int MONEDAS_POR_PUNTO = 50;
    static final String CONCEPTO_COMPRA = "Compra de pista: ";

    private List<RankingDto> ranking(Long partidaId, List<Jugador> participantes,
                                     Map<Long, List<ObjetivoMaster>> objetivosPorJugador, List<PistaMaster> listaPistas) {
        java.util.Set<Long> asesinos = participantes.stream().filter(Jugador::isAsesino).map(Jugador::getId)
                .collect(Collectors.toSet());
        java.util.Set<Long> armas = participantes.stream().filter(j -> j.isAsesino() && j.getArmaDe() != null)
                .map(j -> j.getArmaDe().getId()).collect(Collectors.toSet());
        Map<Long, Long> compradas = transacciones.compras(partidaId, CONCEPTO_COMPRA + "%").stream()
                        .collect(Collectors.groupingBy(t -> t.getDeId(), Collectors.counting()));
        // Acusaciones finales que no señalan a ningún asesino: lo que suma el asesino.
        long libres = participantes.stream()
                .filter(j -> !j.isAsesino() && j.getFinalSospechoso() != null && !asesinos.contains(j.getFinalSospechoso().getId()))
                .count();

        List<RankingDto> filas = new ArrayList<>();
        for (Jugador j : participantes) {
            List<String> desglose = new ArrayList<>();
            int puntos = 0;
            List<ObjetivoMaster> suyos = objetivosPorJugador.getOrDefault(j.getId(), List.of());
            boolean principal = suyos.stream().anyMatch(o -> o.principal() && o.estado() == EstadoObjetivo.CUMPLIDO);
            int secundarios = (int) suyos.stream().filter(o -> !o.principal() && o.estado() == EstadoObjetivo.CUMPLIDO).count();
            if (principal) {
                puntos += PUNTOS_PRINCIPAL;
                desglose.add("Objetivo principal +" + PUNTOS_PRINCIPAL);
            }
            if (secundarios > 0) {
                puntos += secundarios * PUNTOS_SECUNDARIO;
                desglose.add(secundarios + " secundarios +" + secundarios * PUNTOS_SECUNDARIO);
            }
            Boolean aciertaAsesino = null;
            Boolean aciertaArma = null;
            if (j.isAsesino()) {
                if (libres > 0) {
                    puntos += (int) libres * PUNTOS_ASESINO_LIBRE;
                    desglose.add(libres + " acusaciones fallidas +" + libres * PUNTOS_ASESINO_LIBRE);
                }
            } else if (j.getFinalSospechoso() != null) {
                aciertaAsesino = asesinos.contains(j.getFinalSospechoso().getId());
                aciertaArma = j.getFinalArmaDe() != null && armas.contains(j.getFinalArmaDe().getId());
                if (aciertaAsesino) {
                    puntos += PUNTOS_ASESINO;
                    desglose.add("Acierta el asesino +" + PUNTOS_ASESINO);
                }
                if (aciertaArma) {
                    puntos += PUNTOS_ARMA;
                    desglose.add("Acierta el arma +" + PUNTOS_ARMA);
                }
            }
            int nCompradas = compradas.getOrDefault(j.getId(), 0L).intValue();
            if (nCompradas > 0) {
                puntos += nCompradas * PUNTOS_POR_PISTA_COMPRADA;
                desglose.add(nCompradas + " pistas compradas " + nCompradas * PUNTOS_POR_PISTA_COMPRADA);
            }
            int porDinero = j.getDinero() / MONEDAS_POR_PUNTO;
            if (porDinero > 0) {
                puntos += porDinero;
                desglose.add(j.getDinero() + " monedas +" + porDinero);
            }
            int nPistas = (int) listaPistas.stream().filter(p -> p.jugadorIds().contains(j.getId())).count();
            filas.add(new RankingDto(j.getId(), j.getNombre(), j.isAsesino(), puntos,
                    (int) suyos.stream().filter(o -> o.estado() == EstadoObjetivo.CUMPLIDO).count(), suyos.size(),
                    principal, nPistas, nCompradas, j.getDinero(),
                    j.getFinalEn() == null ? null : dtoFinal(j), aciertaAsesino, aciertaArma, desglose));
        }
        filas.sort(Comparator.comparingInt(RankingDto::puntos).reversed().thenComparing(RankingDto::nombre));
        return filas;
    }

    /** Ficha del propio Máster (nombre, foto, profesión...): lo que los jugadores ven de él. */
    public void editarFichaMaster(Long partidaId, Long masterId, FichaEntrada ficha) {
        Jugador master = jugadores.findByIdAndPartidaId(masterId, partidaId)
                .filter(j -> j.getRol() == Rol.MASTER)
                .orElseThrow(() -> noEncontrado("Máster"));
        aplicar(master, ficha);
    }

    public void cambiarFase(Long partidaId, Fase fase) {
        if (fase == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la fase");
        }
        Partida partida = partida(partidaId);
        partida.setFase(fase);
        registrar(partida, "FASE", "Fase cambiada a " + fase);
    }

    /** Deja a ese jugador como único asesino (null = ninguno). Para varios, usar marcarAsesino. */
    public void cambiarAsesino(Long partidaId, Long jugadorId) {
        Partida partida = partida(partidaId);
        Jugador elegido = jugadorId == null ? null : jugador(partidaId, jugadorId);
        for (Jugador j : jugadores.findByPartidaIdAndAsesinoTrueOrderByNombreAsc(partidaId)) {
            if (elegido == null || !j.getId().equals(elegido.getId())) {
                quitarAsesino(j);
            }
        }
        if (elegido == null) {
            registrar(partida, "ASESINO", "Asesino sin asignar");
            return;
        }
        elegido.setAsesino(true);
        registrar(partida, "ASESINO", "Asesino activo: " + elegido.getNombre());
    }

    /** Marca o desmarca a un jugador como asesino. Puede haber varios: se ven entre ellos como cómplices. */
    public void marcarAsesino(Long partidaId, Long jugadorId, boolean asesino) {
        Jugador jugador = jugador(partidaId, jugadorId);
        if (jugador.isAsesino() == asesino) {
            return;
        }
        if (asesino) {
            jugador.setAsesino(true);
        } else {
            quitarAsesino(jugador);
        }
        registrar(jugador.getPartida(), "ASESINO", (asesino ? "Asesino: " : "Ya no es asesino: ") + jugador.getNombre());
    }

    private static void quitarAsesino(Jugador jugador) {
        jugador.setAsesino(false);
        jugador.setArmaDe(null);
    }

    /** El arma se elige mientras dura la convivencia; desde la investigación ya no se puede cambiar. */
    private static boolean armaBloqueada(Partida partida) {
        return partida.getFase() != Fase.PREPARACION && partida.getFase() != Fase.CONVIVENCIA;
    }

    /** El asesino elige la herramienta: la de su oficio o la de otro personaje para incriminarle. */
    public void elegirArma(Long jugadorId, Long armaDeId) {
        Jugador yo = jugadores.findById(jugadorId).orElseThrow(() -> noEncontrado("Jugador"));
        if (!yo.isAsesino()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el asesino elige arma");
        }
        Partida partida = yo.getPartida();
        if (armaBloqueada(partida)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La investigación ha empezado: el arma ya no se puede cambiar");
        }
        if (armaDeId == null) {
            yo.setArmaDe(null);
            return;
        }
        Jugador duenio = jugador(partida.getId(), armaDeId);
        if (duenio.getHerramienta() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, duenio.getNombre() + " no tiene herramienta asignada");
        }
        yo.setArmaDe(duenio);
        registrar(partida, "ARMA", yo.getNombre() + " elige " + duenio.getHerramienta()
                + (duenio.getId().equals(yo.getId()) ? " (la suya)" : " (la de " + duenio.getNombre() + ")"));
    }

    /**
     * Crea o actualiza la pista "El arma" a partir de las armas elegidas por los asesinos.
     * Señala el oficio, no la persona: los jugadores tienen que relacionarlo. Queda bloqueada.
     */
    public Long generarPistaArma(Long partidaId) {
        Partida partida = partida(partidaId);
        List<Jugador> conArma = jugadores.findByPartidaIdAndAsesinoTrueOrderByNombreAsc(partidaId).stream()
                .filter(j -> j.getArmaDe() != null).toList();
        if (conArma.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ningún asesino ha elegido arma todavía");
        }
        StringBuilder texto = new StringBuilder("El atizador estaba limpio porque no fue el arma. El forense encuentra marcas de ");
        for (int i = 0; i < conArma.size(); i++) {
            Jugador duenio = conArma.get(i).getArmaDe();
            if (i > 0) {
                texto.append(i == conArma.size() - 1 ? " y de " : ", de ");
            }
            texto.append(minuscula(duenio.getHerramienta()));
            if (duenio.getProfesion() != null) {
                texto.append(" (algo propio del oficio de ").append(minuscula(duenio.getProfesion())).append(")");
            }
        }
        texto.append(".");
        Pista pista = pistas.findByPartidaIdOrderByIdAsc(partidaId).stream()
                .filter(p -> p.getTitulo().equals(TITULO_ARMA)).findFirst()
                .orElseGet(() -> {
                    Pista nueva = new Pista();
                    nueva.setPartida(partida);
                    nueva.setTitulo(TITULO_ARMA);
                    return nueva;
                });
        pista.setContenido(texto.toString());
        pistas.save(pista);
        registrar(partida, "PISTA", "Pista \"" + TITULO_ARMA + "\" generada con las armas elegidas");
        return pista.getId();
    }

    private static final String TITULO_ARMA = "El arma";

    private static String minuscula(String texto) {
        return texto.isEmpty() ? texto : Character.toLowerCase(texto.charAt(0)) + texto.substring(1);
    }

    // ---------- Jugadores ----------

    public Long crearJugador(Long partidaId, FichaEntrada ficha) {
        Jugador jugador = new Jugador();
        jugador.setPartida(partida(partidaId));
        jugador.setRol(Rol.JUGADOR);
        jugador.setCodigoAcceso(codigos.nuevo());
        aplicar(jugador, ficha);
        return jugadores.save(jugador).getId();
    }

    public void editarJugador(Long partidaId, Long jugadorId, FichaEntrada ficha) {
        aplicar(jugador(partidaId, jugadorId), ficha);
    }

    public void borrarJugador(Long partidaId, Long jugadorId) {
        Jugador jugador = jugador(partidaId, jugadorId);
        for (Jugador otro : jugadores.findByArmaDeId(jugadorId)) {
            otro.setArmaDe(null);
        }
        for (Pista pista : pistas.findByPartidaIdOrderByIdAsc(partidaId)) {
            pista.getDescubiertaPor().remove(jugador);
        }
        // Las pruebas de otros que lo señalaban se conservan, pero sin la persona.
        for (Entrega entrega : entregas.findByPersonaId(jugadorId)) {
            entrega.setPersona(null);
        }
        for (Jugador otro : jugadores.findByFinalSospechosoIdOrFinalArmaDeId(jugadorId, jugadorId)) {
            if (otro.getFinalSospechoso() != null && otro.getFinalSospechoso().getId().equals(jugadorId)) {
                otro.setFinalSospechoso(null);
            }
            if (otro.getFinalArmaDe() != null && otro.getFinalArmaDe().getId().equals(jugadorId)) {
                otro.setFinalArmaDe(null);
            }
        }
        for (Pista pista : pistas.findByAutorFalsaIdOrderByIdAsc(jugadorId)) {
            pista.setAutorFalsa(null);
        }
        for (Pista pista : pistas.findByFalsaParaId(jugadorId)) {
            pista.setFalsaPara(null);
        }
        for (Envio envio : envios.dirigidosA(jugadorId)) {
            envio.getDestinatarios().remove(jugador);
        }
        for (Habilidad h : habilidades.findByPersonaId(jugadorId)) {
            h.setPersona(null);
        }
        habilidades.deleteByJugadorId(jugadorId);
        publicaciones.deleteAll(publicaciones.findByAutorIdOrAcusadoId(jugadorId, jugadorId));
        tratos.deleteAll(tratos.deJugador(jugadorId));
        marcas.deleteByAutorId(jugadorId);
        entregas.deleteByObjetivoJugadorId(jugadorId);
        secretos.deleteByJugadorId(jugadorId);
        objetivos.deleteByJugadorId(jugadorId);
        mensajes.deleteByDestinatarioId(jugadorId);
        anotaciones.deleteByAutorIdOrSobreId(jugadorId, jugadorId);
        jugadores.delete(jugador);
    }

    private void aplicar(Jugador jugador, FichaEntrada ficha) {
        jugador.setNombre(requerido(ficha.nombre(), "nombre"));
        jugador.setEdad(ficha.edad());
        jugador.setProfesion(limpio(ficha.profesion()));
        jugador.setPareja(limpio(ficha.pareja()));
        jugador.setImagenUrl(enlaceImagen(ficha.imagenUrl()));
        jugador.setHerramienta(limpio(ficha.herramienta()));
        jugador.setPersonalidad(limpio(ficha.personalidad()));
        jugador.setRelaciones(limpio(ficha.relaciones()));
        jugador.setContexto(limpio(ficha.contexto()));
        jugador.setLoQueSabeMaster(limpio(ficha.loQueSabeMaster()));
        jugador.setMotivoPotencial(limpio(ficha.motivoPotencial()));
    }

    // ---------- Secretos ----------

    public Long crearSecreto(Long partidaId, Long jugadorId, SecretoEntrada datos) {
        Secreto secreto = new Secreto();
        secreto.setJugador(jugador(partidaId, jugadorId));
        secreto.setTexto(requerido(datos.texto(), "texto"));
        secreto.setRevelado(Boolean.TRUE.equals(datos.revelado()));
        return secretos.save(secreto).getId();
    }

    public void editarSecreto(Long partidaId, Long secretoId, SecretoEntrada datos) {
        Secreto secreto = secreto(partidaId, secretoId);
        if (datos.texto() != null) {
            secreto.setTexto(requerido(datos.texto(), "texto"));
        }
        if (datos.revelado() != null && datos.revelado() != secreto.isRevelado()) {
            secreto.setRevelado(datos.revelado());
            registrar(secreto.getJugador().getPartida(), "SECRETO",
                    (datos.revelado() ? "Secreto revelado de " : "Secreto ocultado de ") + secreto.getJugador().getNombre());
        }
    }

    public void borrarSecreto(Long partidaId, Long secretoId) {
        secretos.delete(secreto(partidaId, secretoId));
    }

    // ---------- Objetivos ----------

    public Long crearObjetivo(Long partidaId, Long jugadorId, ObjetivoEntrada datos) {
        Objetivo objetivo = new Objetivo();
        objetivo.setJugador(jugador(partidaId, jugadorId));
        objetivo.setTexto(requerido(datos.texto(), "texto"));
        if (datos.estado() != null) {
            objetivo.setEstado(datos.estado());
        }
        if (datos.tipo() != null) {
            objetivo.setTipo(datos.tipo());
        }
        if (datos.cantidad() != null) {
            objetivo.setCantidad(cantidadValida(datos.cantidad()));
        }
        if (datos.recompensaDinero() != null) {
            objetivo.setRecompensaDinero(noNegativo(datos.recompensaDinero(), "recompensaDinero"));
        }
        if (Boolean.TRUE.equals(datos.principal())) {
            hacerPrincipal(objetivo, jugadorId);
        }
        return objetivos.save(objetivo).getId();
    }

    public void editarObjetivo(Long partidaId, Long objetivoId, ObjetivoEntrada datos) {
        Objetivo objetivo = objetivo(partidaId, objetivoId);
        if (datos.texto() != null) {
            objetivo.setTexto(requerido(datos.texto(), "texto"));
        }
        if (datos.tipo() != null) {
            objetivo.setTipo(datos.tipo());
        }
        if (datos.cantidad() != null) {
            objetivo.setCantidad(cantidadValida(datos.cantidad()));
        }
        if (datos.recompensaDinero() != null) {
            objetivo.setRecompensaDinero(noNegativo(datos.recompensaDinero(), "recompensaDinero"));
        }
        if (datos.principal() != null) {
            if (datos.principal()) {
                hacerPrincipal(objetivo, objetivo.getJugador().getId());
            } else {
                objetivo.setPrincipal(false);
            }
        }
        if (datos.estado() != null && datos.estado() != objetivo.getEstado()) {
            cambiarEstado(objetivo, datos.estado());
        }
    }

    /** El Máster decide el estado; al dar un objetivo por cumplido se desbloquea su pista de recompensa. */
    private void cambiarEstado(Objetivo objetivo, EstadoObjetivo nuevo) {
        objetivo.setEstado(nuevo);
        Jugador jugador = objetivo.getJugador();
        Partida partida = jugador.getPartida();
        String que = switch (nuevo) {
            case CUMPLIDO -> "Objetivo cumplido por ";
            case FALLIDO -> "Objetivo fallido de ";
            case ENTREGADO -> "Objetivo marcado como entregado para ";
            case ACTIVO -> "Objetivo reabierto para ";
        };
        registrar(partida, "OBJETIVO", que + jugador.getNombre() + ": " + resumen(objetivo.getTexto()));
        Pista premio = objetivo.getRecompensa();
        if (nuevo == EstadoObjetivo.CUMPLIDO && premio != null && premio.getDescubiertaPor().add(jugador)) {
            registrar(partida, "PISTA", "Pista \"" + premio.getTitulo() + "\" desbloqueada para " + jugador.getNombre()
                    + " por cumplir un objetivo");
        }
        if (nuevo == EstadoObjetivo.CUMPLIDO && objetivo.getRecompensaDinero() > 0 && !objetivo.isRecompensaCobrada()) {
            mover(partida, null, jugador, objetivo.getRecompensaDinero(), "Objetivo cumplido: " + resumen(objetivo.getTexto()));
            objetivo.setRecompensaCobrada(true);
        }
    }

    /** Pista que se desbloquea al cumplir el objetivo (null = ninguna). */
    public void recompensaObjetivo(Long partidaId, Long objetivoId, Long pistaId) {
        Objetivo objetivo = objetivo(partidaId, objetivoId);
        objetivo.setRecompensa(pistaId == null ? null : pista(partidaId, pistaId));
    }

    public void borrarObjetivo(Long partidaId, Long objetivoId) {
        Objetivo objetivo = objetivo(partidaId, objetivoId);
        entregas.deleteByObjetivoId(objetivoId);
        objetivos.delete(objetivo);
    }

    /** Solo hay un objetivo principal por jugador: al marcar uno, los demás pasan a secundarios. */
    private void hacerPrincipal(Objetivo objetivo, Long jugadorId) {
        for (Objetivo otro : objetivos.findByJugadorIdOrderByIdAsc(jugadorId)) {
            otro.setPrincipal(false);
        }
        objetivo.setPrincipal(true);
    }

    private static int cantidadValida(int cantidad) {
        if (cantidad < 1 || cantidad > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe estar entre 1 y 20");
        }
        return cantidad;
    }

    // ---------- Pistas ----------

    public Long crearPista(Long partidaId, PistaEntrada datos) {
        Pista pista = new Pista();
        pista.setPartida(partida(partidaId));
        pista.setTitulo(requerido(datos.titulo(), "titulo"));
        pista.setContenido(requerido(datos.contenido(), "contenido"));
        pista.setNota(limpio(datos.nota()));
        return pistas.save(pista).getId();
    }

    public void editarPista(Long partidaId, Long pistaId, PistaEntrada datos) {
        Pista pista = pista(partidaId, pistaId);
        pista.setTitulo(requerido(datos.titulo(), "titulo"));
        pista.setContenido(requerido(datos.contenido(), "contenido"));
        if (datos.nota() != null) {
            pista.setNota(limpio(datos.nota()));
        }
    }

    /** Define exactamente qué jugadores ven la pista (lista vacía = bloqueada para todos). */
    public void visibilidadPista(Long partidaId, Long pistaId, VisibilidadPista datos) {
        Pista pista = pista(partidaId, pistaId);
        List<Jugador> destinatarios = new ArrayList<>();
        if (datos.jugadorIds() != null) {
            for (Long id : datos.jugadorIds()) {
                destinatarios.add(jugador(partidaId, id));
            }
        }
        pista.getDescubiertaPor().clear();
        pista.getDescubiertaPor().addAll(destinatarios);
        registrar(pista.getPartida(), "PISTA", "Pista \"" + pista.getTitulo() + "\" visible para "
                + (destinatarios.isEmpty() ? "nadie"
                : destinatarios.stream().map(Jugador::getNombre).sorted().collect(Collectors.joining(", "))));
    }

    /** Pone la pista a la venta en la tienda del Máster (null = la retira). */
    public void precioPista(Long partidaId, Long pistaId, Integer precio) {
        Pista pista = pista(partidaId, pistaId);
        pista.setPrecio(precio == null ? null : noNegativo(precio, "precio"));
    }

    public void borrarPista(Long partidaId, Long pistaId) {
        Pista pista = pista(partidaId, pistaId);
        for (Objetivo objetivo : objetivos.findByRecompensaId(pistaId)) {
            objetivo.setRecompensa(null);
        }
        envios.deleteAll(envios.findByPistaId(pistaId));
        for (Publicacion p : publicaciones.findByPistaMostradaId(pistaId)) {
            p.setPistaMostrada(null);
        }
        for (Trato t : tratos.conPista(pistaId)) {
            if (t.getPistaOfrecida() != null && t.getPistaOfrecida().getId().equals(pistaId)) {
                t.setPistaOfrecida(null);
            }
            if (t.getPistaRecibida() != null && t.getPistaRecibida().getId().equals(pistaId)) {
                t.setPistaRecibida(null);
            }
        }
        pistas.delete(pista);
    }

    // ---------- Mensajes ----------

    /** Envía un mensaje a los jugadores indicados; sin lista, a todos los jugadores. */
    public int enviarMensaje(Long partidaId, MensajeEntrada datos) {
        String texto = requerido(datos.texto(), "texto");
        List<Jugador> destinatarios = new ArrayList<>();
        if (datos.jugadorIds() == null || datos.jugadorIds().isEmpty()) {
            destinatarios.addAll(jugadores.findByPartidaIdAndRolOrderByNombreAsc(partidaId, Rol.JUGADOR));
        } else {
            for (Long id : datos.jugadorIds()) {
                destinatarios.add(jugador(partidaId, id));
            }
        }
        for (Jugador destinatario : destinatarios) {
            Mensaje mensaje = new Mensaje();
            mensaje.setDestinatario(destinatario);
            mensaje.setTexto(texto);
            mensajes.save(mensaje);
        }
        registrar(partida(partidaId), "MENSAJE", "Mensaje enviado a " + destinatarios.size() + " jugador(es)");
        return destinatarios.size();
    }

    // ---------- Dinero ----------

    /** Un jugador paga a otro participante de su partida (el Máster incluido). */
    public TransaccionDto pagar(Long jugadorId, PagoEntrada datos) {
        Jugador yo = jugadores.findById(jugadorId).orElseThrow(() -> noEncontrado("Jugador"));
        if (datos.jugadorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige a quién pagas");
        }
        if (datos.jugadorId().equals(jugadorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes pagarte a ti mismo");
        }
        Jugador destino = jugadores.findByIdAndPartidaId(datos.jugadorId(), yo.getPartida().getId())
                .orElseThrow(() -> noEncontrado("Jugador"));
        int cantidad = positivo(datos.cantidad());
        exigirSaldo(yo, cantidad);
        String concepto = limpio(datos.concepto());
        if (concepto != null && concepto.length() > 300) {
            concepto = concepto.substring(0, 300);
        }
        return dtoTransaccion(mover(yo.getPartida(), yo, destino, cantidad, concepto));
    }

    /** Compra una pista de la tienda: se descuenta el precio y la pista pasa a ser visible para el comprador. */
    public PistaJugador comprarPista(Long jugadorId, Long pistaId) {
        Jugador yo = jugadores.findById(jugadorId).orElseThrow(() -> noEncontrado("Jugador"));
        Partida partida = yo.getPartida();
        Pista pista = pista(partida.getId(), pistaId);
        if (pista.getPrecio() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esa pista no está a la venta");
        }
        if (pista.getDescubiertaPor().contains(yo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes esa pista");
        }
        exigirSaldo(yo, pista.getPrecio());
        mover(partida, yo, null, pista.getPrecio(), CONCEPTO_COMPRA + pista.getTitulo());
        pista.getDescubiertaPor().add(yo);
        registrar(partida, "PISTA", yo.getNombre() + " compra la pista \"" + pista.getTitulo() + "\" por " + pista.getPrecio());
        return new PistaJugador(pista.getId(), pista.getTitulo(), pista.getContenido());
    }

    /** El Máster da (cantidad positiva) o quita (negativa) dinero a varios jugadores; sin lista, a todos. */
    public int ajustarDinero(Long partidaId, AjusteDinero datos) {
        if (datos.cantidad() == null || datos.cantidad() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica una cantidad distinta de cero");
        }
        Partida partida = partida(partidaId);
        List<Jugador> destinos = new ArrayList<>();
        if (datos.jugadorIds() == null || datos.jugadorIds().isEmpty()) {
            destinos.addAll(jugadores.findByPartidaIdAndRolOrderByNombreAsc(partidaId, Rol.JUGADOR));
        } else {
            for (Long id : datos.jugadorIds()) {
                destinos.add(jugador(partidaId, id));
            }
        }
        int cantidad = datos.cantidad();
        if (Math.abs((long) cantidad) > 1_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad demasiado grande");
        }
        if (cantidad < 0) {
            for (Jugador j : destinos) {
                exigirSaldo(j, -cantidad);
            }
        }
        String concepto = limpio(datos.concepto());
        for (Jugador j : destinos) {
            if (cantidad > 0) {
                mover(partida, null, j, cantidad, concepto == null ? "Del Máster" : concepto);
            } else {
                mover(partida, j, null, -cantidad, concepto == null ? "Retirado por el Máster" : concepto);
            }
        }
        return destinos.size();
    }

    /** Mueve dinero y lo apunta. de/para null = la banca. */
    Transaccion mover(Partida partida, Jugador de, Jugador para, int cantidad, String concepto) {
        if (de != null) {
            de.setDinero(de.getDinero() - cantidad);
        }
        if (para != null) {
            para.setDinero(para.getDinero() + cantidad);
        }
        Transaccion t = transacciones.save(new Transaccion(partida.getId(), de, para, cantidad, concepto));
        registrar(partida, "DINERO", (de == null ? "Banca" : de.getNombre()) + " → "
                + (para == null ? "Banca" : para.getNombre()) + ": " + cantidad + (concepto == null ? "" : " (" + concepto + ")"));
        return t;
    }

    static void exigirSaldo(Jugador jugador, int cantidad) {
        if (jugador.getDinero() < cantidad) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    jugador.getNombre() + " no tiene suficiente dinero (tiene " + jugador.getDinero() + ")");
        }
    }

    static int positivo(Integer cantidad) {
        if (cantidad == null || cantidad <= 0 || cantidad > 1_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser un número mayor que cero");
        }
        return cantidad;
    }

    static int noNegativo(int valor, String campo) {
        if (valor < 0 || valor > 1_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo \"" + campo + "\" no puede ser negativo");
        }
        return valor;
    }

    // ---------- Variables narrativas ----------

    public void guardarVariable(Long partidaId, String clave, String valor) {
        String claveLimpia = requerido(clave, "clave");
        VariableNarrativa variable = variables.findByPartidaIdAndClave(partidaId, claveLimpia).orElseGet(() -> {
            VariableNarrativa nueva = new VariableNarrativa();
            nueva.setPartida(partida(partidaId));
            nueva.setClave(claveLimpia);
            return nueva;
        });
        variable.setValor(valor);
        variables.save(variable);
    }

    public void borrarVariable(Long partidaId, String clave) {
        variables.findByPartidaIdAndClave(partidaId, clave).ifPresent(variables::delete);
    }

    // =====================================================================
    // Utilidades: todo se busca SIEMPRE dentro de la partida del que pregunta
    // =====================================================================

    Partida partida(Long partidaId) {
        return partidas.findById(partidaId).orElseThrow(() -> noEncontrado("Partida"));
    }

    Jugador jugador(Long partidaId, Long jugadorId) {
        return jugadores.findByIdAndPartidaId(jugadorId, partidaId)
                .filter(j -> j.getRol() == Rol.JUGADOR)
                .orElseThrow(() -> noEncontrado("Jugador"));
    }

    private Secreto secreto(Long partidaId, Long secretoId) {
        return secretos.findById(secretoId)
                .filter(s -> s.getJugador().getPartida().getId().equals(partidaId))
                .orElseThrow(() -> noEncontrado("Secreto"));
    }

    private Objetivo objetivo(Long partidaId, Long objetivoId) {
        return objetivos.findById(objetivoId)
                .filter(o -> o.getJugador().getPartida().getId().equals(partidaId))
                .orElseThrow(() -> noEncontrado("Objetivo"));
    }

    Pista pista(Long partidaId, Long pistaId) {
        return pistas.findByIdAndPartidaId(pistaId, partidaId).orElseThrow(() -> noEncontrado("Pista"));
    }

    void registrar(Partida partida, String tipo, String descripcion) {
        eventos.save(new Evento(partida, tipo, descripcion));
    }

    /** Agrupa las entregas por objetivo conservando el orden de creación. */
    private Map<Long, List<EntregaDto>> agrupar(List<Entrega> lista) {
        return lista.stream().collect(Collectors.groupingBy(e -> e.getObjetivo().getId(), LinkedHashMap::new,
                Collectors.mapping(JuegoService::dtoEntrega, Collectors.toList())));
    }

    static ResponseStatusException noEncontrado(String que) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, que + " no encontrado en esta partida");
    }

    static String requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el campo \"" + campo + "\"");
        }
        return valor.trim();
    }

    static String limpio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /** Para el registro de eventos: el texto de un objetivo recortado. */
    static String resumen(String texto) {
        return texto.length() > 70 ? texto.substring(0, 67) + "…" : texto;
    }

    /** Una imagen es un enlace http(s) de hasta 1000 caracteres o una subida a la partida; vacío = sin imagen. */
    static String enlaceImagen(String valor) {
        String url = limpio(valor);
        if (url == null) {
            return null;
        }
        String minusculas = url.toLowerCase();
        boolean web = minusculas.startsWith("http://") || minusculas.startsWith("https://")
                || url.startsWith(ImagenController.PREFIJO);
        if (!web || url.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La foto debe ser un enlace que empiece por http:// o https:// (máximo 1000 caracteres)");
        }
        return url;
    }

    private static PartidaInfo info(Partida p) {
        return new PartidaInfo(p.getId(), p.getNombre(), p.getCasa(), p.getFechaInicio(), p.getFechaFin(), p.getFase());
    }

    private static SecretoDto dtoSecreto(Secreto s) {
        return new SecretoDto(s.getId(), s.getTexto(), s.isRevelado());
    }

    private static EntregaDto dtoEntrega(Entrega e) {
        Jugador persona = e.getPersona();
        return new EntregaDto(e.getId(), persona == null ? null : persona.getId(),
                persona == null ? null : persona.getNombre(), e.getTexto(), e.getCreadoEn());
    }

    static TransaccionDto dtoTransaccion(Transaccion t) {
        return new TransaccionDto(t.getId(), t.getDeId(), t.getDeNombre(), t.getParaId(), t.getParaNombre(),
                t.getCantidad(), t.getConcepto(), t.getCreadoEn());
    }

    /** Estancias de la casa: una por línea, sin vacías. */
    static List<String> lineas(String texto) {
        if (texto == null) {
            return List.of();
        }
        return texto.lines().map(String::strip).filter(l -> !l.isEmpty()).toList();
    }

    /** El Máster ve quién escribe los rumores; los jugadores no. */
    static PublicacionDto dtoPublicacion(Publicacion p, boolean paraMaster) {
        boolean anonimo = p.getTipo() == TipoPublicacion.RUMOR && !paraMaster;
        Jugador autor = anonimo ? null : p.getAutor();
        Pista mostrada = p.getPistaMostrada();
        return new PublicacionDto(p.getId(), p.getTipo(), autor == null ? null : autor.getId(),
                autor == null ? null : autor.getNombre(),
                p.getAcusado() == null ? null : p.getAcusado().getId(),
                p.getAcusado() == null ? null : p.getAcusado().getNombre(),
                p.getTexto(), p.getRespuesta(),
                mostrada == null ? null : new PistaJugador(mostrada.getId(), mostrada.getTitulo(), mostrada.getContenido()),
                p.getCreadaEn(), p.getRespondidaEn());
    }

    static HabilidadDto dtoHabilidad(Habilidad h) {
        return new HabilidadDto(h.getId(), h.getNombre(), h.getDescripcion(), h.getTipo(), h.isPidePersona(), h.getPideTexto(),
                h.getEstado(), h.getPersona() == null ? null : h.getPersona().getId(),
                h.getPersona() == null ? null : h.getPersona().getNombre(), h.getPeticion(), h.getRespuesta(), h.getUsadaEn());
    }

    static AcusacionFinalDto dtoFinal(Jugador j) {
        Jugador sospechoso = j.getFinalSospechoso();
        Jugador arma = j.getFinalArmaDe();
        return new AcusacionFinalDto(j.getId(), j.getNombre(),
                sospechoso == null ? null : sospechoso.getId(), sospechoso == null ? null : sospechoso.getNombre(),
                arma == null ? null : arma.getId(), arma == null ? null : arma.getHerramienta(),
                j.getFinalRazon(), j.getFinalEn());
    }

    private static MensajeDto dtoMensaje(Mensaje m) {
        return new MensajeDto(m.getId(), m.getTexto(), m.getEnviadoEn(), m.isLeido());
    }
}
