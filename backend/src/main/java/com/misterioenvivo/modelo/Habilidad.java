package com.misterioenvivo.modelo;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Habilidad de oficio de un personaje, de un solo uso. El nombre y la descripción son
 * datos de la partida; el tipo dice qué hace el motor al usarla (o si la resuelve el Máster).
 */
@Entity
public class Habilidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jugador_id")
    private Jugador jugador;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(30) not null")
    private TipoHabilidad tipo = TipoHabilidad.MANUAL;

    /** Al usarla hay que elegir a un personaje. */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean pidePersona = false;

    /** Si al usarla hay que escribir algo, la pregunta que se le hace (null = no se escribe nada). */
    @Column(length = 300)
    private String pideTexto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) not null")
    private EstadoHabilidad estado = EstadoHabilidad.DISPONIBLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Jugador persona;

    @Column(length = 2000)
    private String peticion;

    @Column(length = 4000)
    private String respuesta;

    private Instant usadaEn;
    private Instant resueltaEn;

    public Long getId() { return id; }
    public Jugador getJugador() { return jugador; }
    public void setJugador(Jugador jugador) { this.jugador = jugador; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public TipoHabilidad getTipo() { return tipo; }
    public void setTipo(TipoHabilidad tipo) { this.tipo = tipo; }
    public boolean isPidePersona() { return pidePersona; }
    public void setPidePersona(boolean pidePersona) { this.pidePersona = pidePersona; }
    public String getPideTexto() { return pideTexto; }
    public void setPideTexto(String pideTexto) { this.pideTexto = pideTexto; }
    public EstadoHabilidad getEstado() { return estado; }
    public void setEstado(EstadoHabilidad estado) { this.estado = estado; }
    public Jugador getPersona() { return persona; }
    public void setPersona(Jugador persona) { this.persona = persona; }
    public String getPeticion() { return peticion; }
    public void setPeticion(String peticion) { this.peticion = peticion; }
    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
    public Instant getUsadaEn() { return usadaEn; }
    public void setUsadaEn(Instant usadaEn) { this.usadaEn = usadaEn; }
    public Instant getResueltaEn() { return resueltaEn; }
    public void setResueltaEn(Instant resueltaEn) { this.resueltaEn = resueltaEn; }
}
