package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Secreto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecretoRepository extends JpaRepository<Secreto, Long> {
    List<Secreto> findByJugadorIdOrderByIdAsc(Long jugadorId);

    List<Secreto> findByJugadorPartidaIdOrderByIdAsc(Long partidaId);

    void deleteByJugadorId(Long jugadorId);
}
