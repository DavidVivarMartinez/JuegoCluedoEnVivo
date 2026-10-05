package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Jugador;
import com.misterioenvivo.modelo.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JugadorRepository extends JpaRepository<Jugador, Long> {
    @Query("select j from Jugador j join fetch j.partida where j.codigoAcceso = :codigo")
    Optional<Jugador> buscarPorCodigo(@Param("codigo") String codigo);

    boolean existsByCodigoAcceso(String codigoAcceso);

    /** Todos los participantes de la partida, Máster incluido (también es un personaje). */
    List<Jugador> findByPartidaIdOrderByNombreAsc(Long partidaId);

    List<Jugador> findByPartidaIdAndRolOrderByNombreAsc(Long partidaId, Rol rol);

    List<Jugador> findByPartidaIdAndAsesinoTrueOrderByNombreAsc(Long partidaId);

    List<Jugador> findByArmaDeId(Long jugadorId);

    List<Jugador> findByFinalSospechosoIdOrFinalArmaDeId(Long sospechosoId, Long armaDeId);

    Optional<Jugador> findByIdAndPartidaId(Long id, Long partidaId);
}
