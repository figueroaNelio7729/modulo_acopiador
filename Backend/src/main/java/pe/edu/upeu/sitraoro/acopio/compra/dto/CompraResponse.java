package pe.edu.upeu.sitraoro.acopio.compra.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompraResponse(
        Long idCompraG2,
        Long idMinero,
        String nombreMinero,
        String dniMinero,
        LocalDateTime fechaCompra,
        /** Valor del oro: suma de los importes de las partidas. */
        BigDecimal totalPagadoCompraPen,
        List<PartidaResponse> partidas,
        BigDecimal descuentoPen,
        BigDecimal descuentoManualPen,
        /** Valor del oro - descuentos = lo que se entrega al minero. */
        BigDecimal totalNetoPen,
        List<Long> idsPrestamos,
        String descuentoMotivo) {
}
