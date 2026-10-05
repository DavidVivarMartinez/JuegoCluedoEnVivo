package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Publicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {
    List<Publicacion> findTop100ByPartidaIdOrderByIdDesc(Long partidaId);

    List<Publicacion> findByAutorIdOrAcusadoId(Long autorId, Long acusadoId);

    List<Publicacion> findByPistaMostradaId(Long pistaId);
}
