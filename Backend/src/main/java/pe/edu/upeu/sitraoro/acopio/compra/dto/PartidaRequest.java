package pe.edu.upeu.sitraoro.acopio.compra.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PartidaRequest(
        @NotBlank @Pattern(regexp = "ROJO|VERDE", message = "tipoOro debe ser ROJO o VERDE") String tipoOro,
        @NotNull @DecimalMin("0.001") BigDecimal pesoSinFundirG,
        @NotNull @DecimalMin("0.001") BigDecimal pesoFundidoNetoG,
        @NotNull @DecimalMin("0.01") BigDecimal precioGramoPen) {
}
