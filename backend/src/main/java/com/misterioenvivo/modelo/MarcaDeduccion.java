package com.misterioenvivo.modelo;

import jakarta.persistence.*;

/**
 * Casilla del cuaderno de deducción de un jugador: qué piensa de un sospechoso,
 * de un arma o de una estancia. Solo la ve quien la marca.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"autor_id", "categoria", "clave"}))
public class MarcaDeduccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id")
    private Jugador autor;

    /** SOSPECHOSO, ARMA o LUGAR. */
    @Column(nullable = false, length = 20)
    private String categoria;

    /** Id del personaje (sospechoso o dueño del arma) o nombre de la estancia. */
    @Column(nullable = false, length = 200)
    private String clave;

    /** SI (encaja), NO (descartado) o DUDA. */
    @Column(nullable = false, length = 10)
    private String marca;

    public Long getId() { return id; }
    public Jugador getAutor() { return autor; }
    public void setAutor(Jugador autor) { this.autor = autor; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
}
