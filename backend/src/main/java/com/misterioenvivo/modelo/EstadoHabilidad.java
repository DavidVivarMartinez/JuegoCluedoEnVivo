package com.misterioenvivo.modelo;

public enum EstadoHabilidad {
    DISPONIBLE,
    /** Usada; espera la respuesta del Máster. */
    SOLICITADA,
    /** Usada y contestada. El Máster puede reponerla (vuelve a DISPONIBLE). */
    RESUELTA
}
