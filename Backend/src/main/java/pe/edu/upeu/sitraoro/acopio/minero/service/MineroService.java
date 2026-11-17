package pe.edu.upeu.sitraoro.acopio.minero.service;

import java.util.List;

import pe.edu.upeu.sitraoro.acopio.minero.dto.MineroRequest;
import pe.edu.upeu.sitraoro.acopio.minero.dto.MineroResponse;
import pe.edu.upeu.sitraoro.acopio.minero.entity.Minero;

public interface MineroService {
    MineroResponse crear(MineroRequest request);

    List<MineroResponse> listar();

    MineroResponse obtener(Long idMinero);

    MineroResponse actualizar(Long idMinero, MineroRequest request);

    /** 409 si el minero ya tiene compras o adelantos registrados. */
    void eliminar(Long idMinero);

    /** Para otros submodulos (ej. compra): evita acceder al repository ajeno. */
    Minero obtenerEntidad(Long idMinero);
}
