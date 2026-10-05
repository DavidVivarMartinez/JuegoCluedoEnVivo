package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Habilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabilidadRepository extends JpaRepository<Habilidad, Long> {
    List<Habilidad> findByJugadorIdOrderByIdAsc(Long jugadorId);

    List<Habilidad> findByJugadorPartidaIdOrderByIdAsc(Long partidaId);

    List<Habilidad> findByPersonaId(Long personaId);

    void deleteByJugadorId(Long jugadorId);
}
