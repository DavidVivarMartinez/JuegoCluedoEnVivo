package com.misterioenvivo.modelo;

/** Qué tiene que aportar el jugador para entregar un objetivo. */
public enum TipoObjetivo {
    /** Marcar que lo ha conseguido, con una nota opcional para el Máster. */
    LOGRO,
    /** Una respuesta escrita (o varias, según "cantidad"). */
    TEXTO,
    /** Elegir a un personaje y contar qué dijo o hizo (tantas veces como "cantidad"). */
    PERSONA
}
