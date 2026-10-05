package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.Trato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TratoRepository extends JpaRepository<Trato, Long> {
    @Query("select t from Trato t where t.de.id = :jugador or t.para.id = :jugador order by t.id desc")
    List<Trato> deJugador(@Param("jugador") Long jugadorId);

    @Query("select t from Trato t where t.pistaOfrecida.id = :pista or t.pistaRecibida.id = :pista")
    List<Trato> conPista(@Param("pista") Long pistaId);
}
