package pe.edu.upeu.sitraoro.acopio.entrega.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EntregaRequest(
        @NotNull @Size(min = 1, max = 200, message = "Selecciona entre 1 y 200 compras")
        List<@NotNull @Positive Long> idsComprasAcopiador) {
}
