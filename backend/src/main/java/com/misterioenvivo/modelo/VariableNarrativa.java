package com.misterioenvivo.modelo;

import jakarta.persistence.*;

/** Variable libre clave/valor que el Máster usa para controlar la narrativa. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"partida_id", "clave"}))
public class VariableNarrativa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id")
    private Partida partida;

    @Column(nullable = false, length = 100)
    private String clave;

    @Column(length = 4000)
    private String valor;

    public Long getId() { return id; }
    public Partida getPartida() { return partida; }
    public void setPartida(Partida partida) { this.partida = partida; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
}
