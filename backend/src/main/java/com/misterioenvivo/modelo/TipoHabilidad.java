package com.misterioenvivo.modelo;

/** Qué ocurre al usar una habilidad. Las automáticas se resuelven al momento; MANUAL la contesta el Máster. */
public enum TipoHabilidad {
    /** El jugador pide algo (a un personaje, sobre una estancia...) y el Máster responde. */
    MANUAL,
    /** Ver los títulos de las pistas que tiene otro personaje. */
    VER_PISTAS,
    /** Ver los últimos movimientos de dinero de otro personaje. */
    VER_MOVIMIENTOS,
    /** Ver los últimos pagos entre jugadores de toda la partida. */
    RASTREAR_PAGOS,
    /** Llevarse gratis una pista al azar de la tienda. */
    PISTA_AL_AZAR,
    /** Publicar un rumor anónimo en el tablón. */
    RUMOR_ANONIMO
}
