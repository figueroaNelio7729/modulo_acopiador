package pe.edu.upeu.sitraoro.acopio.minero.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.upeu.sitraoro.acopio.minero.entity.Minero;

public interface MineroRepository extends JpaRepository<Minero, Long> {
    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    boolean existsByDocumentoIdentidadAndIdMineroNot(String documentoIdentidad, Long idMinero);
}
