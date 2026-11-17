package pe.edu.upeu.sitraoro.acopio.minero.dto;

import java.time.LocalDateTime;

public record MineroResponse(
        Long idMinero,
        String documentoIdentidad,
        String nombresApellidos,
        String telefono,
        String zonaProcedencia,
        LocalDateTime fechaRegistro) {
}
