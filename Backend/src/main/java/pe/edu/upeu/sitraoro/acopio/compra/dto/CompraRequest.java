package pe.edu.upeu.sitraoro.acopio.compra.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompraRequest(
        @NotNull Long idMinero,
        @NotEmpty @Valid List<PartidaRequest> partidas,
        /** Adelantos PENDIENTES del minero que se descuentan en esta compra. */
        List<Long> idsPrestamos,
        @DecimalMin(value = "0", message = "El descuento no puede ser negativo") BigDecimal descuentoManualPen,
        @Size(max = 200) String motivoDescuento) {
}
