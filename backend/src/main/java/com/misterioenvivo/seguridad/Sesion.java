package com.misterioenvivo.seguridad;

import com.misterioenvivo.modelo.Rol;

/** Quién hace la petición. La resuelve AuthInterceptor a partir del código de acceso. */
public record Sesion(Long jugadorId, Long partidaId, Rol rol) {
    public static final String ATRIBUTO = "sesion";
}
