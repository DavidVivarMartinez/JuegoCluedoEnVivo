package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    List<Transaccion> findTop100ByPartidaIdOrderByIdDesc(Long partidaId);

    @Query("select t from Transaccion t where t.partidaId = :partida and (t.deId = :jugador or t.paraId = :jugador) order by t.id desc")
    List<Transaccion> deJugador(@Param("partida") Long partidaId, @Param("jugador") Long jugadorId);

    /** Pagos de jugadores a la banca cuyo concepto empieza igual (p. ej. las compras de pistas). */
    @Query("select t from Transaccion t where t.partidaId = :partida and t.deId is not null and t.paraId is null and t.concepto like :prefijo")
    List<Transaccion> compras(@Param("partida") Long partidaId, @Param("prefijo") String prefijo);

    /** Pagos entre jugadores (sin la banca), los más recientes primero. */
    List<Transaccion> findTop10ByPartidaIdAndDeIdIsNotNullAndParaIdIsNotNullOrderByIdDesc(Long partidaId);
}
