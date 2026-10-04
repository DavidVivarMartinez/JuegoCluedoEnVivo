package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {
    List<Mensaje> findByDestinatarioIdOrderByIdDesc(Long jugadorId);

    List<Mensaje> findTop100ByDestinatarioPartidaIdOrderByIdDesc(Long partidaId);

    Optional<Mensaje> findByIdAndDestinatarioId(Long id, Long jugadorId);

    void deleteByDestinatarioId(Long jugadorId);
}
