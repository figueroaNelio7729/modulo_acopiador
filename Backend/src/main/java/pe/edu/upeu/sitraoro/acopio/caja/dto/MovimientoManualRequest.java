package pe.edu.upeu.sitraoro.acopio.caja.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record MovimientoManualRequest(
        @NotBlank @Pattern(regexp = "INGRESO|EGRESO", message = "El tipo debe ser INGRESO o EGRESO") String tipo,
        @NotNull @DecimalMin(value = "0", inclusive = false, message = "El monto debe ser mayor que cero")
        @Digits(integer = 14, fraction = 2) BigDecimal monto,
        @NotBlank(message = "Escribe el concepto del movimiento") @Size(max = 200) String concepto,
        @PastOrPresent(message = "La fecha no puede ser futura") LocalDate fecha,
        Boolean saldoInicial) {
}
