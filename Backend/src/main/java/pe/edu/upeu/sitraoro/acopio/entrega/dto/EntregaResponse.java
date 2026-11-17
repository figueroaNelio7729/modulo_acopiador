package pe.edu.upeu.sitraoro.acopio.entrega.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Entrega enviada al mayorista: los pesos son el oro fundido que entrego el acopiador. */
public record EntregaResponse(
        Long idRecepcion,
        LocalDate fecha,
        String nombreAcopiador,
        BigDecimal rojoG,
        BigDecimal verdeG,
        BigDecimal totalG,
        List<Long> idsComprasAcopiador) {
}
