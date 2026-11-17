package pe.edu.upeu.sitraoro.acopio.compra.dto;

import java.math.BigDecimal;

public record PartidaResponse(
        Long idTransaccionG2,
        String tipoOro,
        BigDecimal pesoSinFundirG,
        BigDecimal pesoFundidoNetoG,
        BigDecimal mermaG,
        BigDecimal precioAplicadoPen,
        BigDecimal totalPagadoPen,
        /** N.º de entrega al mayorista, o null si aun no se envio. */
        Long idRecepcionMayorista) {
}
