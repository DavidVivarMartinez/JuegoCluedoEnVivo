package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ImagenRepository extends JpaRepository<Imagen, Long> {
    Optional<Imagen> findByClave(String clave);

    Optional<Imagen> findByPartidaIdAndNombre(Long partidaId, String nombre);

    boolean existsByClave(String clave);

    /** Listado sin los bytes. */
    @Query("select new com.misterioenvivo.repo.ImagenResumen(i.nombre, i.clave, i.tamano) "
            + "from Imagen i where i.partida.id = :partida order by i.nombre")
    List<ImagenResumen> resumenes(@Param("partida") Long partidaId);
}
