package com.misterioenvivo.modelo;

import jakarta.persistence.*;
import java.time.Instant;

/** Prueba que aporta un jugador para un objetivo suyo: un personaje elegido y/o un texto. */
@Entity
public class Entrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objetivo_id")
    private Objetivo objetivo;

    /** Personaje al que se refiere la prueba (objetivos de tipo PERSONA). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Jugador persona;

    @Column(length = 2000)
    private String texto;

    @Column(nullable = false)
    private Instant creadoEn = Instant.now();

    public Long getId() { return id; }
    public Objetivo getObjetivo() { return objetivo; }
    public void setObjetivo(Objetivo objetivo) { this.objetivo = objetivo; }
    public Jugador getPersona() { return persona; }
    public void setPersona(Jugador persona) { this.persona = persona; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public Instant getCreadoEn() { return creadoEn; }
}
