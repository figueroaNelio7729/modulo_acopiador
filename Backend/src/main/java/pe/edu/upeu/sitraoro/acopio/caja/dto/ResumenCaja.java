package pe.edu.upeu.sitraoro.acopio.caja.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenCaja(
        BigDecimal saldoPen,
        BigDecimal ingresosMes,
        BigDecimal egresosMes,
        List<MovimientoResponse> ultimos) {
}
