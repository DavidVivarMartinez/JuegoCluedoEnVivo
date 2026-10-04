package com.misterioenvivo.modelo;

import jakarta.persistence.*;
import java.time.Instant;

/** Mensaje privado del Máster a un jugador (notificación dentro de la app). */
@Entity
public class Mensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id")
    private Jugador destinatario;

    @Column(nullable = false, length = 4000)
    private String texto;

    @Column(nullable = false)
    private Instant enviadoEn = Instant.now();

    @Column(nullable = false)
    private boolean leido = false;

    public Long getId() { return id; }
    public Jugador getDestinatario() { return destinatario; }
    public void setDestinatario(Jugador destinatario) { this.destinatario = destinatario; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public Instant getEnviadoEn() { return enviadoEn; }
    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
}
