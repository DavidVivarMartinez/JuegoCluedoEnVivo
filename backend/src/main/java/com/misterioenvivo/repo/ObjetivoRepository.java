package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Objetivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObjetivoRepository extends JpaRepository<Objetivo, Long> {
    List<Objetivo> findByJugadorIdOrderByIdAsc(Long jugadorId);

    List<Objetivo> findByJugadorPartidaIdOrderByIdAsc(Long partidaId);

    List<Objetivo> findByRecompensaId(Long pistaId);

    void deleteByJugadorId(Long jugadorId);
}
