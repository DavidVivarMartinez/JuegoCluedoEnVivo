package com.misterioenvivo.repo;

/** Datos de una imagen sin sus bytes, para listarlas. */
public record ImagenResumen(String nombre, String clave, int tamano) { }
