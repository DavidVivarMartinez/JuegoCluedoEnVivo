package com.misterioenvivo.modelo;

import jakarta.persistence.*;

/** Participante de una partida. El Máster también es un Jugador con rol MASTER. */
@Entity
public class Jugador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id")
    private Partida partida;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true, length = 20)
    private String codigoAcceso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol = Rol.JUGADOR;

    // --- Ficha visible para el propio jugador ---
    private Integer edad;
    private String profesion;
    private String pareja;
    @Column(length = 4000)
    private String personalidad;
    @Column(length = 4000)
    private String relaciones;
    @Column(length = 4000)
    private String contexto;

    // --- Solo Máster ---
    @Column(length = 4000)
    private String loQueSabeMaster;
    @Column(length = 4000)
    private String motivoPotencial;

    public Long getId() { return id; }
    public Partida getPartida() { return partida; }
    public void setPartida(Partida partida) { this.partida = partida; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCodigoAcceso() { return codigoAcceso; }
    public void setCodigoAcceso(String codigoAcceso) { this.codigoAcceso = codigoAcceso; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }
    public String getProfesion() { return profesion; }
    public void setProfesion(String profesion) { this.profesion = profesion; }
    public String getPareja() { return pareja; }
    public void setPareja(String pareja) { this.pareja = pareja; }
    public String getPersonalidad() { return personalidad; }
    public void setPersonalidad(String personalidad) { this.personalidad = personalidad; }
    public String getRelaciones() { return relaciones; }
    public void setRelaciones(String relaciones) { this.relaciones = relaciones; }
    public String getContexto() { return contexto; }
    public void setContexto(String contexto) { this.contexto = contexto; }
    public String getLoQueSabeMaster() { return loQueSabeMaster; }
    public void setLoQueSabeMaster(String loQueSabeMaster) { this.loQueSabeMaster = loQueSabeMaster; }
    public String getMotivoPotencial() { return motivoPotencial; }
    public void setMotivoPotencial(String motivoPotencial) { this.motivoPotencial = motivoPotencial; }
}
