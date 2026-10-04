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
    public Set<Jugador> getDescubiertaPor() { return descubiertaPor; }
}
