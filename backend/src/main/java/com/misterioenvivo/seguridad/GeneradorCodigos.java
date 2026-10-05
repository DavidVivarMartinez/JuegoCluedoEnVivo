package com.misterioenvivo.seguridad;

import com.misterioenvivo.repo.JugadorRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Genera códigos de acceso únicos, fáciles de teclear en el móvil (sin 0/O, 1/I/L). */
@Component
public class GeneradorCodigos {
    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LONGITUD = 8;

    private final SecureRandom azar = new SecureRandom();
    private final JugadorRepository jugadores;

    public GeneradorCodigos(JugadorRepository jugadores) {
        this.jugadores = jugadores;
    }

    public String nuevo() {
        String codigo;
        do {
            codigo = aleatorio(LONGITUD);
        } while (jugadores.existsByCodigoAcceso(codigo));
        return codigo;
    }

    /** Código con el mismo alfabeto sin comprobar unicidad (la comprueba quien lo usa). */
    public String aleatorio(int longitud) {
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
