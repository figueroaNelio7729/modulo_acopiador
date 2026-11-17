package pe.edu.upeu.sitraoro.acopio.compra.dto;

import java.math.BigDecimal;

public record PrecioOroResponse(
        String tipoOro,
        int leyAplicadaPct,
        BigDecimal precioGramoPen) {
}
