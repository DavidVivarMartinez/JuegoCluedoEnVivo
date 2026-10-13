package com.misterioenvivo.modelo;

/** Qué es cada entrada del tablón público. */
public enum TipoPublicacion {
    /** Un jugador acusa a otro delante de todos; el acusado puede responder. */
    ACUSACION,
    /** Rumor anónimo (habilidad de un personaje). */
    RUMOR,
    /** Aviso del Máster al grupo. */
    AVISO,
    /** Respuesta de un jugador a la encuesta del viernes, revelada por el Máster (con nombre o sin él). */
    ENCUESTA
}
