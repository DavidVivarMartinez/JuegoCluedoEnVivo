package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.VariableNarrativa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VariableRepository extends JpaRepository<VariableNarrativa, Long> {
    List<VariableNarrativa> findByPartidaIdOrderByClaveAsc(Long partidaId);

    Optional<VariableNarrativa> findByPartidaIdAndClave(Long partidaId, String clave);
}
