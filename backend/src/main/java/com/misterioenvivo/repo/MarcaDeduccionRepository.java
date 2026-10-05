package com.misterioenvivo.repo;

import com.misterioenvivo.modelo.MarcaDeduccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarcaDeduccionRepository extends JpaRepository<MarcaDeduccion, Long> {
    List<MarcaDeduccion> findByAutorId(Long autorId);

    Optional<MarcaDeduccion> findByAutorIdAndCategoriaAndClave(Long autorId, String categoria, String clave);

    void deleteByAutorId(Long autorId);
}
