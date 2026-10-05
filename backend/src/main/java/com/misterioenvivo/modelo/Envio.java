package com.misterioenvivo.modelo;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Algo que el Máster hace llegar a unos jugadores: una pista (sobre) o una visión
 * (una imagen sin texto). Puede programarse a una hora; sin hora ni destinatarios
 * es una visión de reserva que el Máster manda cuando quiera.
 */
@Entity
public class Envio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id")
    private Partida partida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) not null")
    private TipoEnvio tipo;

    /** La pista que se desbloquea (tipo PISTA). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pista_id")
    private Pista pista;

    /** La imagen de la visión (tipo VISION). */
    @Column(length = 1000)
    private String imagenUrl;

    /** Para el Máster: qué muestra, qué significa, en qué momento va. Nunca llega a los jugadores. */
    @Column(length = 4000)
    private String nota;

    @ManyToMany
    @JoinTable(name = "envio_jugador",
            joinColumns = @JoinColumn(name = "envio_id"),
            inverseJoinColumns = @JoinColumn(name = "jugador_id"))
    private Set<Jugador> destinatarios = new HashSet<>();

    /** Cuándo se entrega sola (null = solo a mano). */
    private Instant programadaPara;

    /** Cuándo se entregó (null = pendiente). */
    private Instant enviadoEn;

    /** Si los destinatarios ven antes de tiempo un "sobre sellado" con la hora a la que se abre. */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean anunciar = false;

    public Long getId() { return id; }
    public Partida getPartida() { return partida; }
    public void setPartida(Partida partida) { this.partida = partida; }
    public TipoEnvio getTipo() { return tipo; }
    public void setTipo(TipoEnvio tipo) { this.tipo = tipo; }
    public Pista getPista() { return pista; }
    public void setPista(Pista pista) { this.pista = pista; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }
    public Set<Jugador> getDestinatarios() { return destinatarios; }
    public Instant getProgramadaPara() { return programadaPara; }
    public void setProgramadaPara(Instant programadaPara) { this.programadaPara = programadaPara; }
    public Instant getEnviadoEn() { return enviadoEn; }
    public void setEnviadoEn(Instant enviadoEn) { this.enviadoEn = enviadoEn; }
    public boolean isAnunciar() { return anunciar; }
    public void setAnunciar(boolean anunciar) { this.anunciar = anunciar; }
}
