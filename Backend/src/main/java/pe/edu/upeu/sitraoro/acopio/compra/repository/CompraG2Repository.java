package pe.edu.upeu.sitraoro.acopio.compra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.upeu.sitraoro.acopio.compra.entity.CompraG2;

public interface CompraG2Repository extends JpaRepository<CompraG2, Long> {

    /** Trae cabecera + minero + partidas en una sola consulta (evita N+1). */
    @EntityGraph(attributePaths = { "minero", "detalles" })
    List<CompraG2> findByIdCentroAcopioOrderByFechaCompraDescIdCompraG2Desc(Long idCentroAcopio);
}
