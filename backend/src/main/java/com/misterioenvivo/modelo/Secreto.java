package com.misterioenvivo.modelo;

import jakarta.persistence.*;

/** Secreto ficticio de un personaje. Si está revelado, lo ve todo el grupo. */
@Entity
public class Secreto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jugador_id")
    private Jugador jugador;

    @Column(nullable = false, length = 4000)
    private String texto;

    @Column(nullable = false)
    private boolean revelado = false;

    public Long getId() { return id; }
    public Jugador getJugador() { return jugador; }
    public void setJugador(Jugador jugador) { this.jugador = jugador; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public boolean isRevelado() { return revelado; }
    public void setRevelado(boolean revelado) { this.revelado = revelado; }
}
