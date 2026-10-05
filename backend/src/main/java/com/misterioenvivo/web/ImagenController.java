package com.misterioenvivo.web;

import com.misterioenvivo.modelo.Imagen;
import com.misterioenvivo.modelo.Partida;
import com.misterioenvivo.repo.ImagenRepository;
import com.misterioenvivo.repo.PartidaRepository;
import com.misterioenvivo.seguridad.GeneradorCodigos;
import com.misterioenvivo.seguridad.Sesion;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Imágenes de la partida. El Máster las sube (/api/master/imagenes) y cualquiera las ve por
 * su enlace (/api/imagenes/CLAVE), que es lo que se guarda en visiones y fichas.
 */
@RestController
public class ImagenController {

    public static final String PREFIJO = "/api/imagenes/";
    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final int MAXIMO = 5 * 1024 * 1024;

    private final ImagenRepository imagenes;
    private final PartidaRepository partidas;
    private final GeneradorCodigos codigos;

    public ImagenController(ImagenRepository imagenes, PartidaRepository partidas, GeneradorCodigos codigos) {
        this.imagenes = imagenes;
        this.partidas = partidas;
        this.codigos = codigos;
    }

    public record ImagenSubida(String nombre, String url, int tamano) { }

    /** Sube (o reemplaza, si ya hay una con ese nombre) una imagen. El enlace no cambia al reemplazarla. */
    @PostMapping("/api/master/imagenes")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ImagenSubida subir(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        String tipo = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase();
        if (!TIPOS.contains(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se admiten imágenes JPG, PNG, WEBP o GIF");
        }
        if (archivo.isEmpty() || archivo.getSize() > MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen debe pesar menos de 5 MB");
        }
        String nombre = nombreLimpio(archivo.getOriginalFilename());
        Partida partida = partidas.findById(s.partidaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partida no encontrada"));
        Imagen imagen = imagenes.findByPartidaIdAndNombre(partida.getId(), nombre).orElseGet(() -> {
            Imagen nueva = new Imagen();
            nueva.setPartida(partida);
            nueva.setNombre(nombre);
            String clave;
            do {
                clave = codigos.aleatorio(24);
            } while (imagenes.existsByClave(clave));
            nueva.setClave(clave);
            return nueva;
        });
        byte[] datos = archivo.getBytes();
        imagen.setTipo(tipo);
        imagen.setDatos(datos);
        imagen.setTamano(datos.length);
        imagen.setCreadaEn(Instant.now());
        imagenes.save(imagen);
        return new ImagenSubida(nombre, PREFIJO + imagen.getClave(), datos.length);
    }

    @GetMapping("/api/master/imagenes")
    @Transactional(readOnly = true)
    public List<ImagenSubida> listar(@RequestAttribute(Sesion.ATRIBUTO) Sesion s) {
        return imagenes.resumenes(s.partidaId()).stream()
                .map(r -> new ImagenSubida(r.nombre(), PREFIJO + r.clave(), r.tamano())).toList();
    }

    /** Pública: la clave es el permiso. */
    @GetMapping(PREFIJO + "{clave}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> ver(@PathVariable("clave") String clave) {
        Imagen imagen = imagenes.findByClave(clave)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getTipo()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .eTag(imagen.getClave() + "-" + imagen.getTamano() + "-" + imagen.getCreadaEn().toEpochMilli())
                .body(imagen.getDatos());
    }

    /** Solo el nombre del fichero, sin rutas, y con un largo razonable. */
    private static String nombreLimpio(String original) {
        String nombre = original == null ? "" : original.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).strip();
        if (nombre.isEmpty() || nombre.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de fichero no válido");
        }
        return nombre;
    }
}
