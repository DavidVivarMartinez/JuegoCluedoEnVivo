package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findTop100ByPartidaIdOrderByIdDesc(Long partidaId);
}
