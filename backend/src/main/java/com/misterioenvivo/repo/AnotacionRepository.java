package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Anotacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnotacionRepository extends JpaRepository<Anotacion, Long> {
    Optional<Anotacion> findByAutorIdAndSobreId(Long autorId, Long sobreId);

    List<Anotacion> findByAutorId(Long autorId);

    /** Al borrar a un jugador desaparecen las notas que escribió y las que hablaban de él. */
    void deleteByAutorIdOrSobreId(Long autorId, Long sobreId);
}
