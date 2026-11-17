package pe.edu.upeu.sitraoro.acopio.caja.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoResponse(
        Long idMovimiento,
        LocalDate fecha,
        String tipo,
        String origen,
        BigDecimal monto,
        String concepto,
        Long idReferencia,
        boolean anulado,
        boolean anulable) {
}
