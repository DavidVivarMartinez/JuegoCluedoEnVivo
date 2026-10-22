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
 * También guarda los documentos del Máster (PDF y HTML, como su dossier): solo él ve sus enlaces.
 */
@RestController
public class ImagenController {

    public static final String PREFIJO = "/api/imagenes/";
    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> DOCUMENTOS = Set.of("application/pdf", "text/html");
    private static final int MAXIMO = 5 * 1024 * 1024;

    private final ImagenRepository imagenes;
    private final PartidaRepository partidas;
    private final GeneradorCodigos codigos;

    public ImagenController(ImagenRepository imagenes, PartidaRepository partidas, GeneradorCodigos codigos) {
        this.imagenes = imagenes;
        this.partidas = partidas;
        this.codigos = codigos;
    }

    public record ImagenSubida(String nombre, String url, int tamano, String tipo) { }

    /** Sube (o reemplaza, si ya hay una con ese nombre) una imagen. El enlace no cambia al reemplazarla. */
    @PostMapping("/api/master/imagenes")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ImagenSubida subir(@RequestAttribute(Sesion.ATRIBUTO) Sesion s, @RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        String tipo = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase();
        if (tipo.contains(";")) {
            tipo = tipo.substring(0, tipo.indexOf(";")).strip();
        }
        if (!TIPOS.contains(tipo) && !DOCUMENTOS.contains(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se admiten imágenes JPG, PNG, WEBP o GIF, o documentos PDF o HTML");
        }
        if (archivo.isEmpty() || archivo.getSize() > MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe pesar menos de 5 MB");
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
        return new ImagenSubida(nombre, PREFIJO + imagen.getClave(), datos.length, tipo);
    }

    @GetMapping("/api/master/imagenes")
    @Transactional(readOnly = true)
    public List<ImagenSubida> listar(@RequestAttribute(Sesion.ATRIBUTO) Sesion s) {
        return imagenes.resumenes(s.partidaId()).stream()
                .map(r -> new ImagenSubida(r.nombre(), PREFIJO + r.clave(), r.tamano(), r.tipo())).toList();
    }

    /** Pública: la clave es el permiso. */
    @GetMapping(PREFIJO + "{clave}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> ver(@PathVariable("clave") String clave) {
        Imagen imagen = imagenes.findByClave(clave)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
        String etiqueta = imagen.getClave() + "-" + imagen.getTamano() + "-" + imagen.getCreadaEn().toEpochMilli();
        if ("text/html".equals(imagen.getTipo())) {
            // Un documento HTML se abre aislado (sin acceso a los datos de la app) y siempre en su última versión.
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                    .header("Content-Security-Policy", "sandbox allow-scripts allow-popups allow-popups-to-escape-sandbox")
                    .header("X-Content-Type-Options", "nosniff")
                    .cacheControl(CacheControl.noCache().cachePrivate())
                    .eTag(etiqueta)
                    .body(imagen.getDatos());
        }
        boolean documento = DOCUMENTOS.contains(imagen.getTipo());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getTipo()))
                .cacheControl(documento ? CacheControl.noCache().cachePrivate() : CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .eTag(etiqueta)
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
