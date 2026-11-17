package pe.edu.upeu.sitraoro.acopio.prestamo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PrestamoRequest(
        @NotNull(message = "Elige el minero") @Positive Long idMinero,
        @NotNull @DecimalMin(value = "0", inclusive = false, message = "El monto debe ser mayor que cero")
        @Digits(integer = 12, fraction = 2) BigDecimal montoPen,
        @PastOrPresent(message = "La fecha no puede ser futura") LocalDate fecha,
        @Size(max = 200) String observacion) {
}
