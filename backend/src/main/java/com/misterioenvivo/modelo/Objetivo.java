package com.misterioenvivo.modelo;

import jakarta.persistence.*;

@Entity
public class Objetivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jugador_id")
    private Jugador jugador;

    @Column(nullable = false, length = 4000)
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoObjetivo estado = EstadoObjetivo.ACTIVO;

    public Long getId() { return id; }
    public Jugador getJugador() { return jugador; }
    public void setJugador(Jugador jugador) { this.jugador = jugador; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public EstadoObjetivo getEstado() { return estado; }
    public void setEstado(EstadoObjetivo estado) { this.estado = estado; }
}
