package com.misterioenvivo.modelo;

/** Ciclo de una pista falsa: el asesino la paga y el Máster decide si se cuela en la partida. */
public enum EstadoFalsa {
    PENDIENTE,
    APROBADA,
    /** El Máster no la acepta: se devuelve el dinero al asesino. */
    RECHAZADA
}
