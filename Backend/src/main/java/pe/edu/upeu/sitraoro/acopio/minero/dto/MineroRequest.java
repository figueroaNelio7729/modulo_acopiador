package pe.edu.upeu.sitraoro.acopio.minero.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MineroRequest(
        @NotBlank @Size(min = 8, max = 15) String documentoIdentidad,
        @NotBlank @Size(max = 150) String nombresApellidos,
        @Size(max = 20) String telefono,
        @Size(max = 100) String zonaProcedencia) {
}
