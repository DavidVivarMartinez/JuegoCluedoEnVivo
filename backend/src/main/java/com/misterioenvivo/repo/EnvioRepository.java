package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EnvioRepository extends JpaRepository<Envio, Long> {
    List<Envio> findByPartidaIdOrderByIdAsc(Long partidaId);

    /** Programados cuya hora ya ha llegado y siguen sin entregar. */
    List<Envio> findByEnviadoEnIsNullAndProgramadaParaLessThanEqual(Instant ahora);

    @Query("select e from Envio e join e.destinatarios j where j.id = :jugadorId order by e.id")
    List<Envio> dirigidosA(@Param("jugadorId") Long jugadorId);

    List<Envio> findByPistaId(Long pistaId);
}
