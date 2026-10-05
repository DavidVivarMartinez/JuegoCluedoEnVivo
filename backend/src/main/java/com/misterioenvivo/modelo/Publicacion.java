package com.misterioenvivo.modelo;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Lo que se publica en el tablón que ve todo el grupo: acusaciones públicas (con la
 * respuesta del acusado), rumores anónimos y avisos del Máster.
 */
@Entity
public class Publicacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id")
    private Partida partida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) not null")
    private TipoPublicacion tipo;

    /** Quién la escribe. En un rumor solo lo sabe el Máster; en un aviso es el propio Máster. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Jugador autor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acusado_id")
    private Jugador acusado;

    @Column(nullable = false, length = 1000)
    private String texto;

    @Column(length = 1000)
    private String respuesta;

    /** Pista que el acusado enseña al grupo para defenderse. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pista_mostrada_id")
    private Pista pistaMostrada;

    private Instant respondidaEn;

    @Column(nullable = false)
    private Instant creadaEn = Instant.now();

    public Long getId() { return id; }
    public Partida getPartida() { return partida; }
    public void setPartida(Partida partida) { this.partida = partida; }
    public TipoPublicacion getTipo() { return tipo; }
    public void setTipo(TipoPublicacion tipo) { this.tipo = tipo; }
    public Jugador getAutor() { return autor; }
    public void setAutor(Jugador autor) { this.autor = autor; }
    public Jugador getAcusado() { return acusado; }
    public void setAcusado(Jugador acusado) { this.acusado = acusado; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
    public Pista getPistaMostrada() { return pistaMostrada; }
    public void setPistaMostrada(Pista pistaMostrada) { this.pistaMostrada = pistaMostrada; }
    public Instant getRespondidaEn() { return respondidaEn; }
    public void setRespondidaEn(Instant respondidaEn) { this.respondidaEn = respondidaEn; }
    public Instant getCreadaEn() { return creadaEn; }
}
