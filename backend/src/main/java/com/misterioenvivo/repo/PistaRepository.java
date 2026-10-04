package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Pista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PistaRepository extends JpaRepository<Pista, Long> {
    List<Pista> findByPartidaIdOrderByIdAsc(Long partidaId);

    Optional<Pista> findByIdAndPartidaId(Long id, Long partidaId);

    @Query("select p from Pista p join p.descubiertaPor j where j.id = :jugadorId order by p.id")
    List<Pista> visiblesPara(@Param("jugadorId") Long jugadorId);
}
