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
            StringBuilder sb = new StringBuilder(LONGITUD);
            for (int i = 0; i < LONGITUD; i++) {
                sb.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
            }
            codigo = sb.toString();
        } while (jugadores.existsByCodigoAcceso(codigo));
        return codigo;
    }
}
