package com.misterioenvivo.modelo;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Intercambio entre dos jugadores: uno ofrece una pista y/o dinero y pide a cambio
 * una pista (que elige el otro al aceptar) y/o dinero. Solo se ejecuta si el otro acepta.
 * Las pistas se comparten: quien da una pista la sigue teniendo.
 */
@Entity
public class Trato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "de_id")
    private Jugador de;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "para_id")
    private Jugador para;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pista_ofrecida_id")
    private Pista pistaOfrecida;

    @Column(nullable = false, columnDefinition = "integer not null default 0")
    private int dineroOfrecido = 0;

    /** Pide a cambio una pista que el otro elige al aceptar. */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean pidePista = false;

    @Column(nullable = false, columnDefinition = "integer not null default 0")
    private int dineroPedido = 0;

    @Column(length = 300)
    private String mensaje;

    /** La pista que dio el otro al aceptar. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pista_recibida_id")
    private Pista pistaRecibida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) not null")
    private EstadoTrato estado = EstadoTrato.PROPUESTO;

    @Column(nullable = false)
    private Instant creadoEn = Instant.now();

    private Instant cerradoEn;

    public Long getId() { return id; }
    public Jugador getDe() { return de; }
    public void setDe(Jugador de) { this.de = de; }
    public Jugador getPara() { return para; }
    public void setPara(Jugador para) { this.para = para; }
    public Pista getPistaOfrecida() { return pistaOfrecida; }
    public void setPistaOfrecida(Pista pistaOfrecida) { this.pistaOfrecida = pistaOfrecida; }
    public int getDineroOfrecido() { return dineroOfrecido; }
    public void setDineroOfrecido(int dineroOfrecido) { this.dineroOfrecido = dineroOfrecido; }
    public boolean isPidePista() { return pidePista; }
    public void setPidePista(boolean pidePista) { this.pidePista = pidePista; }
    public int getDineroPedido() { return dineroPedido; }
    public void setDineroPedido(int dineroPedido) { this.dineroPedido = dineroPedido; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public Pista getPistaRecibida() { return pistaRecibida; }
    public void setPistaRecibida(Pista pistaRecibida) { this.pistaRecibida = pistaRecibida; }
    public EstadoTrato getEstado() { return estado; }
    public void setEstado(EstadoTrato estado) { this.estado = estado; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getCerradoEn() { return cerradoEn; }
    public void setCerradoEn(Instant cerradoEn) { this.cerradoEn = cerradoEn; }
}
