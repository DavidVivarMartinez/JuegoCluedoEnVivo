package com.misterioenvivo.modelo;

import jakarta.persistence.*;
import java.time.Instant;

/** Registro de lo que ocurre en la partida (solo Máster). */
@Entity
public class Evento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id")
    private Partida partida;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private Instant creadoEn = Instant.now();

    protected Evento() { }

    public Evento(Partida partida, String tipo, String descripcion) {
        this.partida = partida;
        this.tipo = tipo;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public Partida getPartida() { return partida; }
    public String getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public Instant getCreadoEn() { return creadoEn; }
}
