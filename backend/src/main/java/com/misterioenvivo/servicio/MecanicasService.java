package com.misterioenvivo.servicio;

import com.misterioenvivo.modelo.*;
import com.misterioenvivo.repo.*;
import com.misterioenvivo.seguridad.GeneradorCodigos;
import com.misterioenvivo.web.Dtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.misterioenvivo.servicio.JuegoService.*;

/**
 * Mecánicas de juego sobre el estado básico: cuaderno de deducción, acusaciones,
 * tablón, QR, habilidades de oficio, tratos, pistas falsas y envíos programados.
 * Como en JuegoService, ningún texto narrativo vive aquí: solo reglas.
 */
@Service
@Transactional
public class MecanicasService {

    private final JuegoService juego;
    private final JugadorRepository jugadores;
    private final PistaRepository pistas;
    private final MarcaDeduccionRepository marcas;
    private final PublicacionRepository publicaciones;
    private final HabilidadRepository habilidades;
    private final TratoRepository tratos;
    private final EnvioRepository envios;
    private final TransaccionRepository transacciones;
    private final GeneradorCodigos codigos;
    private final DateTimeFormatter formatoHora;
    private final SecureRandom azar = new SecureRandom();

    public MecanicasService(JuegoService juego, JugadorRepository jugadores, PistaRepository pistas,
                            MarcaDeduccionRepository marcas, PublicacionRepository publicaciones,
                            HabilidadRepository habilidades, TratoRepository tratos, EnvioRepository envios,
                            TransaccionRepository transacciones, GeneradorCodigos codigos,
                            @Value("${app.zona-horaria:Europe/Madrid}") String zona) {
        this.juego = juego;
        this.jugadores = jugadores;
        this.pistas = pistas;
        this.marcas = marcas;
        this.publicaciones = publicaciones;
        this.habilidades = habilidades;
        this.tratos = tratos;
        this.envios = envios;
        this.transacciones = transacciones;
        this.codigos = codigos;
        this.formatoHora = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.of("es", "ES")).withZone(ZoneId.of(zona));
    }

    // =====================================================================
    // Reglas de la partida (Máster)
    // =====================================================================

    public void reglas(Long partidaId, ReglasEntrada datos) {
        Partida partida = juego.partida(partidaId);
        if (datos.lugares() != null) {
            String lugares = String.join("\n", lineas(datos.lugares()));
            if (lugares.length() > 4000) {
                throw malo("Demasiadas estancias (máximo 4000 caracteres)");
            }
            partida.setLugares(lugares.isEmpty() ? null : lugares);
        }
        if (datos.precioPistaFalsa() != null) {
            partida.setPrecioPistaFalsa(noNegativo(datos.precioPistaFalsa(), "precioPistaFalsa"));
        }
    }

    // =====================================================================
    // Cuaderno de deducción y acusación final (jugador)
    // =====================================================================

    private static final Set<String> CATEGORIAS = Set.of("SOSPECHOSO", "ARMA", "LUGAR");
    private static final Set<String> MARCAS = Set.of("SI", "NO", "DUDA");

    /** Marca (o borra, con marca null) una casilla del cuaderno. Solo la ve quien la marca. */
    public MarcaDto marcar(Long jugadorId, MarcaEntrada datos) {
        Jugador yo = yo(jugadorId);
        String categoria = requerido(datos.categoria(), "categoria").toUpperCase(Locale.ROOT);
        if (!CATEGORIAS.contains(categoria)) {
            throw malo("Categoría desconocida: " + categoria);
        }
        String clave = requerido(datos.clave(), "clave");
        Long partidaId = yo.getPartida().getId();
        switch (categoria) {
            case "SOSPECHOSO" -> juego.jugador(partidaId, comoId(clave));
            case "ARMA" -> {
                if (juego.jugador(partidaId, comoId(clave)).getHerramienta() == null) {
                    throw malo("Ese personaje no tiene herramienta");
                }
            }
            default -> {
                if (!lineas(yo.getPartida().getLugares()).contains(clave)) {
                    throw malo("Esa estancia no está en la lista de la casa");
                }
            }
        }
        String marca = limpio(datos.marca());
        var existente = marcas.findByAutorIdAndCategoriaAndClave(jugadorId, categoria, clave);
        if (marca == null) {
            existente.ifPresent(marcas::delete);
            return new MarcaDto(categoria, clave, null);
        }
        marca = marca.toUpperCase(Locale.ROOT);
        if (!MARCAS.contains(marca)) {
            throw malo("La marca debe ser SI, NO o DUDA");
        }
        MarcaDeduccion casilla = existente.orElseGet(MarcaDeduccion::new);
        casilla.setAutor(yo);
        casilla.setCategoria(categoria);
        casilla.setClave(clave);
        casilla.setMarca(marca);
        marcas.save(casilla);
        return new MarcaDto(categoria, clave, marca);
    }

    /** Acusación final: el Máster la ve al momento; el grupo, cuando la partida pasa a Finalizada. */
    public AcusacionFinalDto acusacionFinal(Long jugadorId, AcusacionFinalEntrada datos) {
        Jugador yo = yo(jugadorId);
        Partida partida = yo.getPartida();
        if (partida.getFase() == Fase.FINALIZADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La partida ha terminado: ya no se puede cambiar");
        }
        Jugador sospechoso = datos.sospechosoId() == null ? null : juego.jugador(partida.getId(), datos.sospechosoId());
        Jugador arma = datos.armaDeId() == null ? null : juego.jugador(partida.getId(), datos.armaDeId());
        if (arma != null && arma.getHerramienta() == null) {
            throw malo(arma.getNombre() + " no tiene herramienta");
        }
        String razon = largo(limpio(datos.razon()), 2000, "La razón");
        boolean vacia = sospechoso == null && arma == null && razon == null;
        yo.setFinalSospechoso(sospechoso);
        yo.setFinalArmaDe(arma);
        yo.setFinalRazon(razon);
        yo.setFinalEn(vacia ? null : Instant.now());
        juego.registrar(partida, "ACUSACION", vacia ? yo.getNombre() + " retira su acusación final"
                : yo.getNombre() + " entrega su acusación final"
                + (sospechoso == null ? "" : ": " + sospechoso.getNombre())
                + (arma == null ? "" : " con " + minusculaInicial(arma.getHerramienta())));
        return vacia ? null : dtoFinal(yo);
    }

    // =====================================================================
    // Tablón público
    // =====================================================================

    /** Acusa a alguien delante de todo el grupo. */
    public Long acusar(Long jugadorId, AcusacionEntrada datos) {
        Jugador yo = yo(jugadorId);
        if (datos.acusadoId() == null) {
            throw malo("Elige a quién acusas");
        }
        if (datos.acusadoId().equals(jugadorId)) {
            throw malo("No puedes acusarte a ti mismo");
        }
        Jugador acusado = juego.jugador(yo.getPartida().getId(), datos.acusadoId());
        Publicacion p = new Publicacion();
        p.setPartida(yo.getPartida());
        p.setTipo(TipoPublicacion.ACUSACION);
        p.setAutor(yo);
        p.setAcusado(acusado);
        p.setTexto(largo(requerido(datos.texto(), "texto"), 1000, "La acusación"));
        publicaciones.save(p);
        juego.registrar(yo.getPartida(), "TABLON", yo.getNombre() + " acusa en público a " + acusado.getNombre());
        return p.getId();
    }

    /** El acusado contesta una vez, y puede enseñar al grupo una de sus pistas para defenderse. */
    public void responder(Long jugadorId, Long publicacionId, RespuestaEntrada datos) {
        Jugador yo = yo(jugadorId);
        Publicacion p = publicaciones.findById(publicacionId)
                .filter(x -> x.getTipo() == TipoPublicacion.ACUSACION && x.getAcusado() != null
                        && x.getAcusado().getId().equals(jugadorId))
                .orElseThrow(() -> noEncontrado("Acusación"));
        if (p.getRespuesta() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya has respondido a esta acusación");
        }
        p.setRespuesta(largo(requerido(datos.texto(), "texto"), 1000, "La respuesta"));
        if (datos.pistaId() != null) {
            p.setPistaMostrada(pistas.visiblesPara(jugadorId).stream()
                    .filter(x -> x.getId().equals(datos.pistaId())).findFirst()
                    .orElseThrow(() -> malo("Solo puedes enseñar una pista que tengas")));
        }
        p.setRespondidaEn(Instant.now());
        juego.registrar(yo.getPartida(), "TABLON", yo.getNombre() + " responde a la acusación de "
                + p.getAutor().getNombre() + (p.getPistaMostrada() == null ? "" : " y enseña «" + p.getPistaMostrada().getTitulo() + "»"));
    }

    public Long aviso(Long partidaId, Long masterId, String texto) {
        Publicacion p = new Publicacion();
        p.setPartida(juego.partida(partidaId));
        p.setTipo(TipoPublicacion.AVISO);
        p.setAutor(jugadores.findByIdAndPartidaId(masterId, partidaId).orElse(null));
        p.setTexto(largo(requerido(texto, "texto"), 1000, "El aviso"));
        return publicaciones.save(p).getId();
    }

    public void borrarPublicacion(Long partidaId, Long publicacionId) {
        publicaciones.delete(publicaciones.findById(publicacionId)
                .filter(p -> p.getPartida().getId().equals(partidaId))
                .orElseThrow(() -> noEncontrado("Publicación")));
    }

    // =====================================================================
    // QR escondidos por la casa
    // =====================================================================

    /** Activa (genera el código) o quita el QR de una pista. Devuelve el código. */
    public String qr(Long partidaId, Long pistaId, boolean activo) {
        Pista pista = juego.pista(partidaId, pistaId);
        if (!activo) {
            pista.setCodigoQr(null);
            return null;
        }
        if (pista.getCodigoQr() == null) {
            String codigo;
            do {
                codigo = codigos.aleatorio(6);
            } while (pistas.existsByCodigoQr(codigo));
            pista.setCodigoQr(codigo);
        }
        return pista.getCodigoQr();
    }

    /** Quien escanea (o teclea) el código se queda con la pista. */
    public PistaJugador canjearQr(Long jugadorId, String codigo) {
        Jugador yo = jugadores.findById(jugadorId).orElseThrow(() -> noEncontrado("Jugador"));
        String limpio = requerido(codigo, "codigo").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        Pista pista = pistas.findByCodigoQrAndPartidaId(limpio, yo.getPartida().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ese código no corresponde a ninguna pista de esta partida"));
        if (yo.getRol() == Rol.JUGADOR && pista.getDescubiertaPor().add(yo)) {
            juego.registrar(yo.getPartida(), "QR", yo.getNombre() + " encuentra el QR de «" + pista.getTitulo() + "»");
        }
        return new PistaJugador(pista.getId(), pista.getTitulo(), pista.getContenido());
    }

    // =====================================================================
    // Habilidades de oficio
    // =====================================================================

    public Long crearHabilidad(Long partidaId, Long jugadorId, HabilidadEntrada datos) {
        Habilidad h = new Habilidad();
        h.setJugador(juego.jugador(partidaId, jugadorId));
        aplicar(h, datos);
        return habilidades.save(h).getId();
    }

    public void editarHabilidad(Long partidaId, Long habilidadId, HabilidadEntrada datos) {
        aplicar(habilidad(partidaId, habilidadId), datos);
    }

    public void borrarHabilidad(Long partidaId, Long habilidadId) {
        habilidades.delete(habilidad(partidaId, habilidadId));
    }

    private static void aplicar(Habilidad h, HabilidadEntrada datos) {
        h.setNombre(largo(requerido(datos.nombre(), "nombre"), 200, "El nombre"));
        h.setDescripcion(largo(requerido(datos.descripcion(), "descripcion"), 2000, "La descripción"));
        if (datos.tipo() != null) {
            h.setTipo(datos.tipo());
        }
        h.setPidePersona(Boolean.TRUE.equals(datos.pidePersona()));
        h.setPideTexto(largo(limpio(datos.pideTexto()), 300, "La pregunta"));
    }

    /** El Máster contesta una habilidad pendiente, o la repone para que se pueda volver a usar. */
    public void resolverHabilidad(Long partidaId, Long habilidadId, ResolverHabilidad datos) {
        Habilidad h = habilidad(partidaId, habilidadId);
        if (datos.estado() == EstadoHabilidad.DISPONIBLE) {
            h.setEstado(EstadoHabilidad.DISPONIBLE);
            h.setPersona(null);
            h.setPeticion(null);
            h.setRespuesta(null);
            h.setUsadaEn(null);
            h.setResueltaEn(null);
            juego.registrar(h.getJugador().getPartida(), "HABILIDAD", "«" + h.getNombre() + "» repuesta a " + h.getJugador().getNombre());
            return;
        }
        h.setRespuesta(largo(requerido(datos.respuesta(), "respuesta"), 4000, "La respuesta"));
        h.setEstado(EstadoHabilidad.RESUELTA);
        h.setResueltaEn(Instant.now());
        juego.registrar(h.getJugador().getPartida(), "HABILIDAD",
                "Contestada «" + h.getNombre() + "» de " + h.getJugador().getNombre());
    }

    /** Usa una habilidad propia. Las automáticas se resuelven al momento; las MANUAL esperan al Máster. */
    public HabilidadDto usarHabilidad(Long jugadorId, Long habilidadId, UsoHabilidad datos) {
        Habilidad h = habilidades.findById(habilidadId)
                .filter(x -> x.getJugador().getId().equals(jugadorId))
                .orElseThrow(() -> noEncontrado("Habilidad"));
        if (h.getEstado() != EstadoHabilidad.DISPONIBLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya has usado esta habilidad");
        }
        Jugador yo = h.getJugador();
        Partida partida = yo.getPartida();
        TipoHabilidad tipo = h.getTipo();

        boolean pidePersona = h.isPidePersona() || tipo == TipoHabilidad.VER_PISTAS || tipo == TipoHabilidad.VER_MOVIMIENTOS;
        Jugador persona = null;
        if (pidePersona) {
            if (datos.personaId() == null) {
                throw malo("Elige a un personaje");
            }
            if (datos.personaId().equals(jugadorId)) {
                throw malo("Tienes que elegir a otra persona");
            }
            persona = juego.jugador(partida.getId(), datos.personaId());
        }
        boolean pideTexto = h.getPideTexto() != null || tipo == TipoHabilidad.RUMOR_ANONIMO;
        String texto = pideTexto ? largo(requerido(datos.texto(), "texto"), 2000, "El texto") : null;

        String respuesta = switch (tipo) {
            case MANUAL -> null;
            case VER_PISTAS -> {
                List<String> titulos = pistas.visiblesPara(persona.getId()).stream().map(Pista::getTitulo).toList();
                yield titulos.isEmpty() ? persona.getNombre() + " no tiene ninguna pista todavía."
                        : "Pistas de " + persona.getNombre() + ":\n· " + String.join("\n· ", titulos);
            }
            case VER_MOVIMIENTOS -> {
                List<Transaccion> lista = transacciones.deJugador(partida.getId(), persona.getId()).stream().limit(10).toList();
                yield lista.isEmpty() ? persona.getNombre() + " no ha movido dinero todavía."
                        : "Últimos movimientos de " + persona.getNombre() + " (tiene " + persona.getDinero() + "):\n"
                        + lista.stream().map(this::linea).collect(Collectors.joining("\n"));
            }
            case RASTREAR_PAGOS -> {
                List<Transaccion> lista = transacciones.findTop10ByPartidaIdAndDeIdIsNotNullAndParaIdIsNotNullOrderByIdDesc(partida.getId());
                yield lista.isEmpty() ? "Nadie ha pagado a nadie todavía."
                        : "Últimos pagos entre jugadores:\n" + lista.stream().map(this::linea).collect(Collectors.joining("\n"));
            }
            case PISTA_AL_AZAR -> {
                List<Pista> candidatas = pistas.findByPartidaIdOrderByIdAsc(partida.getId()).stream()
                        .filter(p -> p.getPrecio() != null && !p.getDescubiertaPor().contains(yo)).toList();
                if (candidatas.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ahora mismo no hay nada que recuperar: prueba más tarde");
                }
                Pista elegida = candidatas.get(azar.nextInt(candidatas.size()));
                elegida.getDescubiertaPor().add(yo);
                yield "Has recuperado «" + elegida.getTitulo() + "». La tienes en Pistas.";
            }
            case RUMOR_ANONIMO -> {
                Publicacion rumor = new Publicacion();
                rumor.setPartida(partida);
                rumor.setTipo(TipoPublicacion.RUMOR);
                rumor.setAutor(yo);
                rumor.setTexto(largo(texto, 1000, "El rumor"));
                publicaciones.save(rumor);
                yield "Rumor publicado en el tablón. Nadie sabrá que fuiste tú.";
            }
        };

        h.setPersona(persona);
        h.setPeticion(texto);
        h.setUsadaEn(Instant.now());
        if (respuesta == null) {
            h.setEstado(EstadoHabilidad.SOLICITADA);
        } else {
            h.setRespuesta(respuesta);
            h.setEstado(EstadoHabilidad.RESUELTA);
            h.setResueltaEn(Instant.now());
        }
        juego.registrar(partida, "HABILIDAD", yo.getNombre() + " usa «" + h.getNombre() + "»"
                + (persona == null ? "" : " sobre " + persona.getNombre())
                + (respuesta == null ? " (espera tu respuesta)" : ""));
        return dtoHabilidad(h);
    }

    private String linea(Transaccion t) {
        return formatoHora.format(t.getCreadoEn()) + " · " + (t.getDeNombre() == null ? "Banca" : t.getDeNombre())
                + " → " + (t.getParaNombre() == null ? "Banca" : t.getParaNombre()) + ": " + t.getCantidad()
                + (t.getConcepto() == null ? "" : " (" + t.getConcepto() + ")");
    }

    private Habilidad habilidad(Long partidaId, Long habilidadId) {
        return habilidades.findById(habilidadId)
                .filter(h -> h.getJugador().getPartida().getId().equals(partidaId))
                .orElseThrow(() -> noEncontrado("Habilidad"));
    }

    // =====================================================================
    // Tratos entre jugadores
    // =====================================================================

    public Long proponerTrato(Long jugadorId, TratoEntrada datos) {
        Jugador yo = yo(jugadorId);
        if (datos.paraId() == null) {
            throw malo("Elige con quién haces el trato");
        }
        if (datos.paraId().equals(jugadorId)) {
            throw malo("No puedes hacer un trato contigo mismo");
        }
        Jugador para = juego.jugador(yo.getPartida().getId(), datos.paraId());
        Pista ofrecida = null;
        if (datos.pistaId() != null) {
            ofrecida = pistas.visiblesPara(jugadorId).stream().filter(p -> p.getId().equals(datos.pistaId())).findFirst()
                    .orElseThrow(() -> malo("Solo puedes ofrecer una pista que tengas"));
        }
        int dinero = datos.dinero() == null ? 0 : noNegativo(datos.dinero(), "dinero");
        int dineroPedido = datos.dineroPedido() == null ? 0 : noNegativo(datos.dineroPedido(), "dineroPedido");
        boolean pidePista = Boolean.TRUE.equals(datos.pidePista());
        if ((ofrecida == null && dinero == 0) || (!pidePista && dineroPedido == 0)) {
            throw malo("Un trato tiene que ofrecer algo y pedir algo a cambio");
        }
        exigirSaldo(yo, dinero);

        Trato t = new Trato();
        t.setDe(yo);
        t.setPara(para);
        t.setPistaOfrecida(ofrecida);
        t.setDineroOfrecido(dinero);
        t.setPidePista(pidePista);
        t.setDineroPedido(dineroPedido);
        t.setMensaje(largo(limpio(datos.mensaje()), 300, "El mensaje"));
        tratos.save(t);
        juego.registrar(yo.getPartida(), "TRATO", yo.getNombre() + " propone un trato a " + para.getNombre());
        return t.getId();
    }

    /** Solo lo acepta el destinatario. Todo se comprueba de nuevo: saldos y pistas pueden haber cambiado. */
    public void aceptarTrato(Long jugadorId, Long tratoId, AceptarTrato datos) {
        Trato t = tratoAbierto(tratoId, jugadorId, false);
        Jugador yo = t.getPara();
        Jugador de = t.getDe();
        Partida partida = yo.getPartida();

        java.util.Set<Long> suyas = pistas.visiblesPara(de.getId()).stream().map(Pista::getId).collect(Collectors.toSet());
        if (t.getPistaOfrecida() != null && !suyas.contains(t.getPistaOfrecida().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, de.getNombre() + " ya no tiene la pista que ofrecía");
        }
        Pista doy = null;
        if (t.isPidePista()) {
            if (datos == null || datos.pistaId() == null) {
                throw malo("Elige qué pista le das a cambio");
            }
            doy = pistas.visiblesPara(jugadorId).stream().filter(p -> p.getId().equals(datos.pistaId())).findFirst()
                    .orElseThrow(() -> malo("Solo puedes dar una pista que tengas"));
            if (suyas.contains(doy.getId())) {
                throw malo(de.getNombre() + " ya tiene esa pista: elige otra");
            }
        }
        exigirSaldo(de, t.getDineroOfrecido());
        exigirSaldo(yo, t.getDineroPedido());

        if (t.getDineroOfrecido() > 0) {
            juego.mover(partida, de, yo, t.getDineroOfrecido(), "Trato con " + yo.getNombre());
        }
        if (t.getDineroPedido() > 0) {
            juego.mover(partida, yo, de, t.getDineroPedido(), "Trato con " + de.getNombre());
        }
        if (t.getPistaOfrecida() != null) {
            t.getPistaOfrecida().getDescubiertaPor().add(yo);
        }
        if (doy != null) {
            doy.getDescubiertaPor().add(de);
            t.setPistaRecibida(doy);
        }
        cerrar(t, EstadoTrato.ACEPTADO);
        juego.registrar(partida, "TRATO", yo.getNombre() + " acepta el trato de " + de.getNombre()
                + (t.getPistaOfrecida() == null ? "" : ": recibe «" + t.getPistaOfrecida().getTitulo() + "»")
                + (doy == null ? "" : " y da «" + doy.getTitulo() + "»"));
    }

    public void rechazarTrato(Long jugadorId, Long tratoId) {
        Trato t = tratoAbierto(tratoId, jugadorId, false);
        cerrar(t, EstadoTrato.RECHAZADO);
        juego.registrar(t.getDe().getPartida(), "TRATO", t.getPara().getNombre() + " rechaza el trato de " + t.getDe().getNombre());
    }

    public void cancelarTrato(Long jugadorId, Long tratoId) {
        cerrar(tratoAbierto(tratoId, jugadorId, true), EstadoTrato.CANCELADO);
    }

    private Trato tratoAbierto(Long tratoId, Long jugadorId, boolean comoProponente) {
        Trato t = tratos.findById(tratoId)
                .filter(x -> (comoProponente ? x.getDe() : x.getPara()).getId().equals(jugadorId))
                .orElseThrow(() -> noEncontrado("Trato"));
        if (t.getEstado() != EstadoTrato.PROPUESTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese trato ya está cerrado");
        }
        return t;
    }

    private static void cerrar(Trato t, EstadoTrato estado) {
        t.setEstado(estado);
        t.setCerradoEn(Instant.now());
    }

    // =====================================================================
    // Pistas falsas (solo asesinos)
    // =====================================================================

    /**
     * El asesino paga y propone una pista inventada. Nadie la ve hasta que el Máster la aprueba;
     * entonces es una pista más, sin ninguna marca de que sea falsa.
     */
    public Long proponerFalsa(Long jugadorId, PistaFalsaEntrada datos) {
        Jugador yo = yo(jugadorId);
        if (!yo.isAsesino()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el asesino puede falsificar pistas");
        }
        Partida partida = yo.getPartida();
        Jugador para = null;
        if (datos.paraId() != null) {
            if (datos.paraId().equals(jugadorId)) {
                throw malo("Elige a otra persona o déjala en la tienda");
            }
            para = juego.jugador(partida.getId(), datos.paraId());
        }
        Pista pista = new Pista();
        pista.setPartida(partida);
        pista.setTitulo(largo(requerido(datos.titulo(), "titulo"), 200, "El título"));
        pista.setContenido(largo(requerido(datos.contenido(), "contenido"), 4000, "El contenido"));
        int precio = partida.getPrecioPistaFalsa();
        exigirSaldo(yo, precio);
        if (precio > 0) {
            // Concepto neutro: otros pueden llegar a ver sus movimientos.
            juego.mover(partida, yo, null, precio, "Encargo al Máster");
        }
        pista.setFalsa(true);
        pista.setAutorFalsa(yo);
        pista.setFalsaPara(para);
        pista.setEstadoFalsa(EstadoFalsa.PENDIENTE);
        pista.setPagadoFalsa(precio);
        pistas.save(pista);
        juego.registrar(partida, "FALSA", yo.getNombre() + " encarga la pista falsa «" + pista.getTitulo() + "» para "
                + (para == null ? "la tienda" : para.getNombre()));
        return pista.getId();
    }

    public void aprobarFalsa(Long partidaId, Long pistaId, AprobarFalsa datos) {
        Pista pista = falsaPendiente(partidaId, pistaId);
        if (datos.titulo() != null) {
            pista.setTitulo(largo(requerido(datos.titulo(), "titulo"), 200, "El título"));
        }
        if (datos.contenido() != null) {
            pista.setContenido(largo(requerido(datos.contenido(), "contenido"), 4000, "El contenido"));
        }
        if (pista.getFalsaPara() != null) {
            pista.getDescubiertaPor().add(pista.getFalsaPara());
        } else {
            if (datos.precio() == null) {
                throw malo("Pon el precio al que se vende en la tienda");
            }
            pista.setPrecio(noNegativo(datos.precio(), "precio"));
        }
        pista.setEstadoFalsa(EstadoFalsa.APROBADA);
        juego.registrar(pista.getPartida(), "FALSA", "Pista falsa «" + pista.getTitulo() + "» aprobada");
    }

    /** No pasa el filtro del Máster: se le devuelve el dinero al asesino. */
    public void rechazarFalsa(Long partidaId, Long pistaId) {
        Pista pista = falsaPendiente(partidaId, pistaId);
        pista.setEstadoFalsa(EstadoFalsa.RECHAZADA);
        if (pista.getAutorFalsa() != null && pista.getPagadoFalsa() > 0) {
            juego.mover(pista.getPartida(), null, pista.getAutorFalsa(), pista.getPagadoFalsa(), "Devolución del Máster");
        }
        juego.registrar(pista.getPartida(), "FALSA", "Pista falsa «" + pista.getTitulo() + "» rechazada");
    }

    private Pista falsaPendiente(Long partidaId, Long pistaId) {
        Pista pista = juego.pista(partidaId, pistaId);
        if (!pista.isFalsa() || pista.getEstadoFalsa() != EstadoFalsa.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esa pista no es una falsa pendiente");
        }
        return pista;
    }

    // =====================================================================
    // Envíos: sobres programados y visiones
    // =====================================================================

    public Long crearEnvio(Long partidaId, EnvioEntrada datos) {
        Envio envio = new Envio();
        envio.setPartida(juego.partida(partidaId));
        aplicar(envio, partidaId, datos);
        return envios.save(envio).getId();
    }

    public void editarEnvio(Long partidaId, Long envioId, EnvioEntrada datos) {
        aplicar(envio(partidaId, envioId), partidaId, datos);
    }

    public void borrarEnvio(Long partidaId, Long envioId) {
        envios.delete(envio(partidaId, envioId));
    }

    private void aplicar(Envio envio, Long partidaId, EnvioEntrada datos) {
        if (datos.tipo() == null) {
            throw malo("Falta el tipo (PISTA o VISION)");
        }
        envio.setTipo(datos.tipo());
        if (datos.tipo() == TipoEnvio.PISTA) {
            if (datos.pistaId() == null) {
                throw malo("Elige la pista del sobre");
            }
            envio.setPista(juego.pista(partidaId, datos.pistaId()));
            envio.setImagenUrl(null);
        } else {
            envio.setPista(null);
            envio.setImagenUrl(enlaceImagen(datos.imagenUrl()));
        }
        envio.setNota(largo(limpio(datos.nota()), 4000, "La nota"));
        envio.getDestinatarios().clear();
        if (datos.jugadorIds() != null) {
            for (Long id : datos.jugadorIds()) {
                envio.getDestinatarios().add(juego.jugador(partidaId, id));
            }
        }
        envio.setProgramadaPara(datos.programadaPara());
        envio.setAnunciar(Boolean.TRUE.equals(datos.anunciar()));
    }

    /**
     * Entrega ya. Con jugadores indicados se manda una copia solo a ellos (así una visión
     * de reserva o un sobre se pueden mandar a quien sea, las veces que haga falta).
     */
    public int enviarAhora(Long partidaId, Long envioId, EnviarAhora datos) {
        Envio envio = envio(partidaId, envioId);
        if (datos != null && datos.jugadorIds() != null && !datos.jugadorIds().isEmpty()) {
            Envio copia = new Envio();
            copia.setPartida(envio.getPartida());
            copia.setTipo(envio.getTipo());
            copia.setPista(envio.getPista());
            copia.setImagenUrl(envio.getImagenUrl());
            copia.setNota(envio.getNota());
            for (Long id : datos.jugadorIds()) {
                copia.getDestinatarios().add(juego.jugador(partidaId, id));
            }
            entregar(copia);
            envios.save(copia);
            return copia.getDestinatarios().size();
        }
        if (envio.getEnviadoEn() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya se entregó");
        }
        entregar(envio);
        return envio.getDestinatarios().size();
    }

    /** Lo llama el programador cada pocos segundos: entrega lo que ya toca. Lo incompleto se queda esperando. */
    public int entregarPendientes() {
        int entregados = 0;
        for (Envio envio : envios.findByEnviadoEnIsNullAndProgramadaParaLessThanEqual(Instant.now())) {
            if (listo(envio)) {
                entregar(envio);
                entregados++;
            }
        }
        return entregados;
    }

    private static boolean listo(Envio envio) {
        return !envio.getDestinatarios().isEmpty()
                && (envio.getTipo() == TipoEnvio.PISTA ? envio.getPista() != null : envio.getImagenUrl() != null);
    }

    private void entregar(Envio envio) {
        if (envio.getDestinatarios().isEmpty()) {
            throw malo("Elige a quién se lo mandas");
        }
        if (envio.getTipo() == TipoEnvio.VISION && envio.getImagenUrl() == null) {
            throw malo("A esta visión le falta la imagen");
        }
        if (envio.getTipo() == TipoEnvio.PISTA) {
            envio.getPista().getDescubiertaPor().addAll(envio.getDestinatarios());
        }
        envio.setEnviadoEn(Instant.now());
        String quienes = envio.getDestinatarios().stream().map(Jugador::getNombre).sorted().collect(Collectors.joining(", "));
        juego.registrar(envio.getPartida(), envio.getTipo() == TipoEnvio.PISTA ? "SOBRE" : "VISION",
                (envio.getTipo() == TipoEnvio.PISTA ? "Sobre «" + envio.getPista().getTitulo() + "» abierto para " : "Visión enviada a ")
                        + quienes);
    }

    private Envio envio(Long partidaId, Long envioId) {
        return envios.findById(envioId)
                .filter(e -> e.getPartida().getId().equals(partidaId))
                .orElseThrow(() -> noEncontrado("Envío"));
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    /** Solo los jugadores (no el Máster) juegan estas mecánicas. */
    private Jugador yo(Long jugadorId) {
        Jugador yo = jugadores.findById(jugadorId).orElseThrow(() -> noEncontrado("Jugador"));
        if (yo.getRol() != Rol.JUGADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esto es cosa de los jugadores");
        }
        return yo;
    }

    private static Long comoId(String clave) {
        try {
            return Long.valueOf(clave);
        } catch (NumberFormatException e) {
            throw malo("Personaje no válido");
        }
    }

    private static String largo(String texto, int maximo, String que) {
        if (texto != null && texto.length() > maximo) {
            throw malo(que + " es demasiado largo (máximo " + maximo + " caracteres)");
        }
        return texto;
    }

    private static String minusculaInicial(String texto) {
        return texto.isEmpty() ? texto : Character.toLowerCase(texto.charAt(0)) + texto.substring(1);
    }

    private static ResponseStatusException malo(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
