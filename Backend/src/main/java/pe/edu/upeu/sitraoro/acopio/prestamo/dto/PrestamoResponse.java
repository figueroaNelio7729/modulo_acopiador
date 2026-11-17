package pe.edu.upeu.sitraoro.acopio.prestamo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PrestamoResponse(
        Long idPrestamo,
        Long idMinero,
        String minero,
        String documentoMinero,
        LocalDate fecha,
        BigDecimal montoPen,
        String observacion,
        String estado,
        Long idCompraG2) {
}
