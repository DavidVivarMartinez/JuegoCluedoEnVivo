package com.misterioenvivo.modelo;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Imagen subida por el Máster (visiones, fotos de personajes). Se guarda en la base de datos
 * para que viaje con la partida a cualquier servidor, y se sirve por una clave que no se puede
 * adivinar: el enlace funciona en un <img> sin código de acceso, pero nadie da con él por azar.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"partida_id", "nombre"}))
public class Imagen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id")
    private Partida partida;

    @Column(nullable = false, unique = true, length = 40)
    private String clave;

    /** Nombre del fichero original: con él se empareja al volver a importar. */
    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false)
    private int tamano;

    // VARBINARY y no @Lob: así es bytea en Postgres, longblob en MySQL y binario en H2.
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, length = 8_000_000)
    private byte[] datos;

    @Column(nullable = false)
    private Instant creadaEn = Instant.now();

    public Long getId() { return id; }
    public Partida getPartida() { return partida; }
    public void setPartida(Partida partida) { this.partida = partida; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getTamano() { return tamano; }
    public void setTamano(int tamano) { this.tamano = tamano; }
    public byte[] getDatos() { return datos; }
    public void setDatos(byte[] datos) { this.datos = datos; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant creadaEn) { this.creadaEn = creadaEn; }
}
