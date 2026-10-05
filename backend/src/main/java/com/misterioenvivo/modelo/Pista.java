package com.misterioenvivo.modelo;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/** Pista de la partida. Solo la ven los jugadores para los que el Máster la desbloquea. */
@Entity
public class Pista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id")
    private Partida partida;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, length = 4000)
    private String contenido;

    /** Precio en la tienda del Máster (null = no está a la venta). */
    private Integer precio;

    /** Solo para el Máster: dónde se esconde, cuándo se da... */
    @Column(length = 2000)
    private String nota;

    /** Código del QR escondido en la casa: quien lo escanea desbloquea la pista (null = sin QR). */
    @Column(unique = true, length = 20)
    private String codigoQr;

    // --- Pista falsa: la propone un asesino, el Máster la aprueba. Los jugadores nunca saben que es falsa ---
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean falsa = false;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_falsa_id")
    private Jugador autorFalsa;
    /** A quién quiere colársela (null = a la tienda). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "falsa_para_id")
    private Jugador falsaPara;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    private EstadoFalsa estadoFalsa;
    /** Lo que pagó el asesino (se le devuelve si el Máster la rechaza). */
    @Column(nullable = false, columnDefinition = "integer not null default 0")
    private int pagadoFalsa = 0;

    @ManyToMany
    @JoinTable(name = "pista_jugador",
            joinColumns = @JoinColumn(name = "pista_id"),
            inverseJoinColumns = @JoinColumn(name = "jugador_id"))
    private Set<Jugador> descubiertaPor = new HashSet<>();

    public Long getId() { return id; }
    public Partida getPartida() { return partida; }
    public void setPartida(Partida partida) { this.partida = partida; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public Integer getPrecio() { return precio; }
    public void setPrecio(Integer precio) { this.precio = precio; }
    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }
    public String getCodigoQr() { return codigoQr; }
    public void setCodigoQr(String codigoQr) { this.codigoQr = codigoQr; }
    public boolean isFalsa() { return falsa; }
    public void setFalsa(boolean falsa) { this.falsa = falsa; }
    public Jugador getAutorFalsa() { return autorFalsa; }
    public void setAutorFalsa(Jugador autorFalsa) { this.autorFalsa = autorFalsa; }
    public Jugador getFalsaPara() { return falsaPara; }
    public void setFalsaPara(Jugador falsaPara) { this.falsaPara = falsaPara; }
    public EstadoFalsa getEstadoFalsa() { return estadoFalsa; }
    public void setEstadoFalsa(EstadoFalsa estadoFalsa) { this.estadoFalsa = estadoFalsa; }
    public int getPagadoFalsa() { return pagadoFalsa; }
    public void setPagadoFalsa(int pagadoFalsa) { this.pagadoFalsa = pagadoFalsa; }
    public Set<Jugador> getDescubiertaPor() { return descubiertaPor; }
}
