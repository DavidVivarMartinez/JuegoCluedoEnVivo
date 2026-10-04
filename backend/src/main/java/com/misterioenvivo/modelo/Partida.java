package com.misterioenvivo.modelo;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/** Una partida concreta: un grupo, una casa, unas fechas. */
@Entity
public class Partida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String casa;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Fase fase = Fase.PREPARACION;

    /** Asesino activo de la partida. Se decide durante el juego, nunca en código. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asesino_id")
    private Jugador asesino;

    @Column(nullable = false)
    private Instant creadaEn = Instant.now();

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCasa() { return casa; }
    public void setCasa(String casa) { this.casa = casa; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public Fase getFase() { return fase; }
    public void setFase(Fase fase) { this.fase = fase; }
    public Jugador getAsesino() { return asesino; }
    public void setAsesino(Jugador asesino) { this.asesino = asesino; }
    public Instant getCreadaEn() { return creadaEn; }
}
