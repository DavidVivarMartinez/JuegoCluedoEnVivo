package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    List<Entrega> findByObjetivoIdOrderByIdAsc(Long objetivoId);

    List<Entrega> findByObjetivoJugadorIdOrderByIdAsc(Long jugadorId);

    List<Entrega> findByObjetivoJugadorPartidaIdOrderByIdAsc(Long partidaId);

    /** Entregas de otros jugadores que señalan a este personaje. */
    List<Entrega> findByPersonaId(Long personaId);

    long countByObjetivoId(Long objetivoId);

    void deleteByObjetivoId(Long objetivoId);

    void deleteByObjetivoJugadorId(Long jugadorId);
}
