package com.misterioenvivo.modelo;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Cuaderno privado: lo que un jugador apunta sobre otro personaje durante la
 * partida. Solo lo ve quien lo escribe; ni el Máster ni el resto lo reciben.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"autor_id", "sobre_id"}))
public class Anotacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id")
    private Jugador autor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sobre_id")
    private Jugador sobre;

    @Column(nullable = false, length = 4000)
    private String texto = "";

    @Column(nullable = false)
    private Instant actualizadoEn = Instant.now();

    public Long getId() { return id; }
    public Jugador getAutor() { return autor; }
    public void setAutor(Jugador autor) { this.autor = autor; }
    public Jugador getSobre() { return sobre; }
    public void setSobre(Jugador sobre) { this.sobre = sobre; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
