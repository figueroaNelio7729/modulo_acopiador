package pe.edu.upeu.sitraoro.acopio.entrega.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Partida de oro comprada que todavia puede enviarse al mayorista. */
public record DisponibleResponse(
        Long idTransaccionG2,
        Long idCompraG2,
        String tipoOro,
        Long idMinero,
        String minero,
        String documentoMinero,
        BigDecimal pesoSinFundirG,
        BigDecimal pesoFundidoNetoG,
        BigDecimal precioAplicadoPen,
        BigDecimal totalPagadoPen,
        LocalDateTime fecha) {
}
