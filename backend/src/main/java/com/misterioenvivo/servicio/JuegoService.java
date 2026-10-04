package com.misterioenvivo.servicio;

import com.misterioenvivo.modelo.*;
import com.misterioenvivo.repo.*;
import com.misterioenvivo.seguridad.GeneradorCodigos;
import com.misterioenvivo.web.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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
    private final PistaRepository pistas;
    private final MensajeRepository mensajes;
    private final EventoRepository eventos;
    private final VariableRepository variables;
    private final GeneradorCodigos codigos;

    public JuegoService(PartidaRepository partidas, JugadorRepository jugadores, SecretoRepository secretos,
                        ObjetivoRepository objetivos, PistaRepository pistas, MensajeRepository mensajes,
                        EventoRepository eventos, VariableRepository variables, GeneradorCodigos codigos) {
        this.partidas = partidas;
        this.jugadores = jugadores;
        this.secretos = secretos;
        this.objetivos = objetivos;
        this.pistas = pistas;
        this.mensajes = mensajes;
        this.eventos = eventos;
        this.variables = variables;
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

        boolean esAsesino = partida.getAsesino() != null && partida.getAsesino().getId().equals(yo.getId());

        List<SecretoRevelado> revelados = secretos.findByJugadorPartidaIdOrderByIdAsc(partidaId).stream()
                .filter(Secreto::isRevelado)
                .map(s -> new SecretoRevelado(s.getJugador().getNombre(), s.getTexto()))
                .toList();

        return new PanelJugador(
                info(partida),
                new Personaje(yo.getNombre(), yo.getEdad(), yo.getProfesion(), yo.getPareja(),
                        yo.getPersonalidad(), yo.getRelaciones(), yo.getContexto()),
                esAsesino,
                secretos.findByJugadorIdOrderByIdAsc(jugadorId).stream().map(JuegoService::dtoSecreto).toList(),
                revelados,
                pistas.visiblesPara(jugadorId).stream()
                        .map(p -> new PistaJugador(p.getId(), p.getTitulo(), p.getContenido())).toList(),
                objetivos.findByJugadorIdOrderByIdAsc(jugadorId).stream().map(JuegoService::dtoObjetivo).toList(),
                mensajes.findByDestinatarioIdOrderByIdDesc(jugadorId).stream().map(JuegoService::dtoMensaje).toList(),
                jugadores.findByPartidaIdOrderByNombreAsc(partidaId).stream().map(Jugador::getNombre).toList());
    }

    public void marcarLeido(Long jugadorId, Long mensajeId) {
        Mensaje mensaje = mensajes.findByIdAndDestinatarioId(mensajeId, jugadorId)
                .orElseThrow(() -> noEncontrado("Mensaje"));
        mensaje.setLeido(true);
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
        Map<Long, List<ObjetivoDto>> objetivosPorJugador = objetivos.findByJugadorPartidaIdOrderByIdAsc(partidaId).stream()
                .collect(Collectors.groupingBy(o -> o.getJugador().getId(),
                        Collectors.mapping(JuegoService::dtoObjetivo, Collectors.toList())));
        List<Mensaje> ultimosMensajes = mensajes.findTop100ByDestinatarioPartidaIdOrderByIdDesc(partidaId);
        Map<Long, Long> sinLeer = ultimosMensajes.stream()
                .filter(m -> !m.isLeido())
                .collect(Collectors.groupingBy(m -> m.getDestinatario().getId(), Collectors.counting()));

        List<JugadorMaster> listaJugadores = jugadores.findByPartidaIdAndRolOrderByNombreAsc(partidaId, Rol.JUGADOR).stream()
                .map(j -> new JugadorMaster(j.getId(), j.getNombre(), j.getCodigoAcceso(), j.getEdad(),
                        j.getProfesion(), j.getPareja(), j.getPersonalidad(), j.getRelaciones(), j.getContexto(),
                        j.getLoQueSabeMaster(), j.getMotivoPotencial(),
                        secretosPorJugador.getOrDefault(j.getId(), List.of()),
                        objetivosPorJugador.getOrDefault(j.getId(), List.of()),
                        sinLeer.getOrDefault(j.getId(), 0L)))
                .toList();

        List<PistaMaster> listaPistas = pistas.findByPartidaIdOrderByIdAsc(partidaId).stream()
                .map(p -> new PistaMaster(p.getId(), p.getTitulo(), p.getContenido(),
                        p.getDescubiertaPor().stream().map(Jugador::getId).sorted().toList()))
                .toList();

        return new EstadoMaster(
                info(partida),
                partida.getAsesino() == null ? null : partida.getAsesino().getId(),
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
                        .toList());
    }

    public void cambiarFase(Long partidaId, Fase fase) {
        if (fase == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la fase");
        }
        Partida partida = partida(partidaId);
        partida.setFase(fase);
        registrar(partida, "FASE", "Fase cambiada a " + fase);
    }

    /** Asigna (o quita, con null) el asesino activo. */
    public void cambiarAsesino(Long partidaId, Long jugadorId) {
        Partida partida = partida(partidaId);
        if (jugadorId == null) {
            partida.setAsesino(null);
            registrar(partida, "ASESINO", "Asesino sin asignar");
            return;
        }
        Jugador jugador = jugador(partidaId, jugadorId);
        partida.setAsesino(jugador);
        registrar(partida, "ASESINO", "Asesino activo: " + jugador.getNombre());
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
        Partida partida = jugador.getPartida();
        if (partida.getAsesino() != null && partida.getAsesino().getId().equals(jugadorId)) {
            partida.setAsesino(null);
        }
        for (Pista pista : pistas.findByPartidaIdOrderByIdAsc(partidaId)) {
            pista.getDescubiertaPor().remove(jugador);
        }
        secretos.deleteByJugadorId(jugadorId);
        objetivos.deleteByJugadorId(jugadorId);
        mensajes.deleteByDestinatarioId(jugadorId);
        jugadores.delete(jugador);
    }

    private void aplicar(Jugador jugador, FichaEntrada ficha) {
        jugador.setNombre(requerido(ficha.nombre(), "nombre"));
        jugador.setEdad(ficha.edad());
        jugador.setProfesion(limpio(ficha.profesion()));
        jugador.setPareja(limpio(ficha.pareja()));
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
        return objetivos.save(objetivo).getId();
    }

    public void editarObjetivo(Long partidaId, Long objetivoId, ObjetivoEntrada datos) {
        Objetivo objetivo = objetivo(partidaId, objetivoId);
        if (datos.texto() != null) {
            objetivo.setTexto(requerido(datos.texto(), "texto"));
        }
        if (datos.estado() != null) {
            objetivo.setEstado(datos.estado());
        }
    }

    public void borrarObjetivo(Long partidaId, Long objetivoId) {
        objetivos.delete(objetivo(partidaId, objetivoId));
    }

    // ---------- Pistas ----------

    public Long crearPista(Long partidaId, PistaEntrada datos) {
        Pista pista = new Pista();
        pista.setPartida(partida(partidaId));
        pista.setTitulo(requerido(datos.titulo(), "titulo"));
        pista.setContenido(requerido(datos.contenido(), "contenido"));
        return pistas.save(pista).getId();
    }

    public void editarPista(Long partidaId, Long pistaId, PistaEntrada datos) {
        Pista pista = pista(partidaId, pistaId);
        pista.setTitulo(requerido(datos.titulo(), "titulo"));
        pista.setContenido(requerido(datos.contenido(), "contenido"));
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

    public void borrarPista(Long partidaId, Long pistaId) {
        pistas.delete(pista(partidaId, pistaId));
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

    private Partida partida(Long partidaId) {
        return partidas.findById(partidaId).orElseThrow(() -> noEncontrado("Partida"));
    }

    private Jugador jugador(Long partidaId, Long jugadorId) {
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

    private Pista pista(Long partidaId, Long pistaId) {
        return pistas.findByIdAndPartidaId(pistaId, partidaId).orElseThrow(() -> noEncontrado("Pista"));
    }

    private void registrar(Partida partida, String tipo, String descripcion) {
        eventos.save(new Evento(partida, tipo, descripcion));
    }

    private static ResponseStatusException noEncontrado(String que) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, que + " no encontrado en esta partida");
    }

    private static String requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el campo \"" + campo + "\"");
        }
        return valor.trim();
    }

    private static String limpio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static PartidaInfo info(Partida p) {
        return new PartidaInfo(p.getId(), p.getNombre(), p.getCasa(), p.getFechaInicio(), p.getFechaFin(), p.getFase());
    }

    private static SecretoDto dtoSecreto(Secreto s) {
        return new SecretoDto(s.getId(), s.getTexto(), s.isRevelado());
    }

    private static ObjetivoDto dtoObjetivo(Objetivo o) {
        return new ObjetivoDto(o.getId(), o.getTexto(), o.getEstado());
    }

    private static MensajeDto dtoMensaje(Mensaje m) {
        return new MensajeDto(m.getId(), m.getTexto(), m.getEnviadoEn(), m.isLeido());
    }
}
