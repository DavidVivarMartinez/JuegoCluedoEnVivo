package com.misterioenvivo.modelo;

/** Ciclo de un objetivo: el jugador lo entrega y el Máster lo da por cumplido o fallido. */
public enum EstadoObjetivo {
    ACTIVO,
    /** El jugador ha aportado sus pruebas y espera la validación del Máster. */
    ENTREGADO,
    CUMPLIDO,
    FALLIDO
}
