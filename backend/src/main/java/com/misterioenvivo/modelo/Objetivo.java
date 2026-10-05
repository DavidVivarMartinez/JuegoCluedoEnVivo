package com.misterioenvivo.modelo;

import jakarta.persistence.*;

/**
 * Objetivo de un personaje. Hay uno principal por jugador; el resto son secundarios.
 * El jugador lo "entrega" aportando pruebas (ver Entrega) y el Máster lo valida.
 */
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

    // Sin restricción CHECK de valores: así se pueden añadir estados sin tocar las bases ya creadas.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) not null")
    private EstadoObjetivo estado = EstadoObjetivo.ACTIVO;

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean principal = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) not null default 'LOGRO'")
    private TipoObjetivo tipo = TipoObjetivo.LOGRO;

    /** Entregas necesarias (personas o respuestas) para darlo por entregado. */
    @Column(nullable = false, columnDefinition = "integer not null default 1")
    private int cantidad = 1;

    /** Pista que se desbloquea al jugador cuando el Máster da el objetivo por cumplido. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recompensa_pista_id")
    private Pista recompensa;

    /** Monedas que recibe el jugador cuando el Máster da el objetivo por cumplido. */
    @Column(nullable = false, columnDefinition = "integer not null default 0")
    private int recompensaDinero = 0;

    /** Si ya se pagaron las monedas: reabrir y volver a validar no paga otra vez. */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean recompensaCobrada = false;

    public Long getId() { return id; }
    public Jugador getJugador() { return jugador; }
    public void setJugador(Jugador jugador) { this.jugador = jugador; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public EstadoObjetivo getEstado() { return estado; }
    public void setEstado(EstadoObjetivo estado) { this.estado = estado; }
    public boolean isPrincipal() { return principal; }
    public void setPrincipal(boolean principal) { this.principal = principal; }
    public TipoObjetivo getTipo() { return tipo; }
    public void setTipo(TipoObjetivo tipo) { this.tipo = tipo; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public int getRecompensaDinero() { return recompensaDinero; }
    public void setRecompensaDinero(int recompensaDinero) { this.recompensaDinero = recompensaDinero; }
    public boolean isRecompensaCobrada() { return recompensaCobrada; }
    public void setRecompensaCobrada(boolean recompensaCobrada) { this.recompensaCobrada = recompensaCobrada; }
    public Pista getRecompensa() { return recompensa; }
    public void setRecompensa(Pista recompensa) { this.recompensa = recompensa; }
}
