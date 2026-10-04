package com.misterioenvivo.seguridad;

import com.misterioenvivo.modelo.Jugador;
import com.misterioenvivo.modelo.Rol;
import com.misterioenvivo.repo.JugadorRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Permisos de la API:
 *  - /api/admin/**  -> cabecera X-Admin-Key (crear partidas / casas).
 *  - /api/master/** -> código de acceso de un Máster.
 *  - /api/yo/**     -> código de acceso de cualquier participante.
 * El código viaja en "Authorization: Bearer CODIGO".
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JugadorRepository jugadores;
    private final String adminKey;

    public AuthInterceptor(JugadorRepository jugadores, @Value("${app.admin-key}") String adminKey) {
        this.jugadores = jugadores;
        this.adminKey = adminKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String ruta = request.getRequestURI();

        if (ruta.startsWith("/api/admin")) {
            String clave = request.getHeader("X-Admin-Key");
            if (clave == null || !iguales(clave, adminKey)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Clave de administración incorrecta");
            }
            return true;
        }

        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta el código de acceso");
        }
        String codigo = cabecera.substring("Bearer ".length()).trim().toUpperCase();
        Jugador jugador = jugadores.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código de acceso no válido"));

        if (ruta.startsWith("/api/master") && jugador.getRol() != Rol.MASTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el Máster puede hacer esto");
        }
        request.setAttribute(Sesion.ATRIBUTO,
                new Sesion(jugador.getId(), jugador.getPartida().getId(), jugador.getRol()));
        return true;
    }

    private static boolean iguales(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
