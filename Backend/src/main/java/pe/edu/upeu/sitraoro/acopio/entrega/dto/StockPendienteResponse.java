package pe.edu.upeu.sitraoro.acopio.entrega.dto;

import java.math.BigDecimal;

/** Stock acumulado pendiente de envio al mayorista, por color. */
public record StockPendienteResponse(
        BigDecimal rojoG,
        BigDecimal rojoPen,
        BigDecimal verdeG,
        BigDecimal verdePen) {
}
