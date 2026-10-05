package com.misterioenvivo.modelo;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Movimiento de dinero del juego. "de" o "para" vacíos = la banca (el Máster).
 * Guarda ids y nombres sin clave ajena para que el historial sobreviva si se borra a alguien.
 */
@Entity
public class Transaccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long partidaId;

    private Long deId;
    private String deNombre;
    private Long paraId;
    private String paraNombre;

    @Column(nullable = false)
    private int cantidad;

    @Column(length = 300)
    private String concepto;

    @Column(nullable = false)
    private Instant creadoEn = Instant.now();

    protected Transaccion() { }

    public Transaccion(Long partidaId, Jugador de, Jugador para, int cantidad, String concepto) {
        this.partidaId = partidaId;
        if (de != null) { this.deId = de.getId(); this.deNombre = de.getNombre(); }
        if (para != null) { this.paraId = para.getId(); this.paraNombre = para.getNombre(); }
        this.cantidad = cantidad;
        this.concepto = concepto;
    }

    public Long getId() { return id; }
    public Long getPartidaId() { return partidaId; }
    public Long getDeId() { return deId; }
    public String getDeNombre() { return deNombre; }
    public Long getParaId() { return paraId; }
    public String getParaNombre() { return paraNombre; }
    public int getCantidad() { return cantidad; }
    public String getConcepto() { return concepto; }
    public Instant getCreadoEn() { return creadoEn; }
}
