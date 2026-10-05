package com.misterioenvivo.modelo;

import jakarta.persistence.*;
import java.time.Instant;

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
    @Column(nullable = false, columnDefinition = "varchar(20) not null")
    private Rol rol = Rol.JUGADOR;

    // --- Ficha pública: la ven todos los personajes de la partida ---
    private Integer edad;
    private String profesion;
    private String pareja;
    /** Enlace a la foto del personaje (http/https). */
    @Column(length = 1000)
    private String imagenUrl;

    /** Herramienta de su oficio: la que usaría si fuera el asesino (o la que otro usaría para incriminarle). */
    @Column(length = 200)
    private String herramienta;

    // --- Estado de juego ---
    /** Puede haber más de un asesino (cómplices). Lo decide el Máster durante la partida. */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean asesino = false;
    /** Si es asesino: de quién es la herramienta que usa (la suya o la de otro para incriminarle). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arma_de_id")
    private Jugador armaDe;
    /** Dinero del juego (monedas). Nunca baja de cero. */
    @Column(nullable = false, columnDefinition = "integer not null default 0")
    private int dinero = 0;

    // --- Acusación final: el Máster la ve siempre; el grupo, cuando la partida termina ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_sospechoso_id")
    private Jugador finalSospechoso;
    /** Dueño de la herramienta que cree que se usó como arma. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_arma_de_id")
    private Jugador finalArmaDe;
    @Column(length = 2000)
    private String finalRazon;
    private Instant finalEn;

    // --- Ficha visible solo para el propio jugador ---
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
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public String getHerramienta() { return herramienta; }
    public void setHerramienta(String herramienta) { this.herramienta = herramienta; }
    public boolean isAsesino() { return asesino; }
    public void setAsesino(boolean asesino) { this.asesino = asesino; }
    public Jugador getArmaDe() { return armaDe; }
    public void setArmaDe(Jugador armaDe) { this.armaDe = armaDe; }
    public int getDinero() { return dinero; }
    public void setDinero(int dinero) { this.dinero = dinero; }
    public Jugador getFinalSospechoso() { return finalSospechoso; }
    public void setFinalSospechoso(Jugador finalSospechoso) { this.finalSospechoso = finalSospechoso; }
    public Jugador getFinalArmaDe() { return finalArmaDe; }
    public void setFinalArmaDe(Jugador finalArmaDe) { this.finalArmaDe = finalArmaDe; }
    public String getFinalRazon() { return finalRazon; }
    public void setFinalRazon(String finalRazon) { this.finalRazon = finalRazon; }
    public Instant getFinalEn() { return finalEn; }
    public void setFinalEn(Instant finalEn) { this.finalEn = finalEn; }
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
