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

    // Sin tipo ENUM nativo ni CHECK: así se pueden añadir fases sin tocar las bases ya creadas.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(30) not null")
    private Fase fase = Fase.PREPARACION;

    @Column(nullable = false)
    private Instant creadaEn = Instant.now();

    /** Estancias de la casa para el cuaderno de deducción, una por línea. */
    @Column(length = 4000)
    private String lugares;

    /** Lo que paga un asesino por colar una pista falsa. */
    @Column(nullable = false, columnDefinition = "integer not null default 200")
    private int precioPistaFalsa = 200;

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
    public Instant getCreadaEn() { return creadaEn; }
    public String getLugares() { return lugares; }
    public void setLugares(String lugares) { this.lugares = lugares; }
    public int getPrecioPistaFalsa() { return precioPistaFalsa; }
    public void setPrecioPistaFalsa(int precioPistaFalsa) { this.precioPistaFalsa = precioPistaFalsa; }
}
