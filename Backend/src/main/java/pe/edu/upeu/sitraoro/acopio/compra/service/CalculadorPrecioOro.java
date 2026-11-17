package pe.edu.upeu.sitraoro.acopio.compra.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

/** Concentra toda la matematica del acopio: precio por gramo, merma e importe. */
@Component
public class CalculadorPrecioOro {

    private static final BigDecimal GRAMOS_POR_ONZA_TROY = new BigDecimal("31.0999");

    /**
     * PrecioGramo = (OnzaUSD * TipoCambio * Ley * (1 - Margen/100)) / 31.0999
     * ROJO: ley 0.90 | VERDE: ley 0.80
     */
    public BigDecimal calcularPrecioGramoPen(String tipoOro, BigDecimal onzaUsd,
            BigDecimal tipoCambioPen, BigDecimal margenExportacionPct) {
        BigDecimal ley = "ROJO".equalsIgnoreCase(tipoOro) ? new BigDecimal("0.90") : new BigDecimal("0.80");
        BigDecimal factorExportacion = BigDecimal.ONE.subtract(margenExportacionPct.movePointLeft(2));
        return onzaUsd.multiply(tipoCambioPen).multiply(ley).multiply(factorExportacion)
                .divide(GRAMOS_POR_ONZA_TROY, 2, RoundingMode.HALF_UP);
    }

    /** Merma (g) = peso sin fundir - peso fundido neto. */
    public BigDecimal calcularMermaGramos(BigDecimal pesoSinFundirG, BigDecimal pesoFundidoNetoG) {
        if (pesoFundidoNetoG.compareTo(pesoSinFundirG) > 0) {
            throw new IllegalArgumentException("El peso fundido (" + pesoFundidoNetoG
                    + " g) no puede superar al peso sin fundir (" + pesoSinFundirG + " g)");
        }
        return pesoSinFundirG.subtract(pesoFundidoNetoG);
    }

    /** Importe (S/) = peso fundido neto (g) * precio por gramo (S/). */
    public BigDecimal calcularImportePartida(BigDecimal pesoFundidoNetoG, BigDecimal precioPorGramoPen) {
        return pesoFundidoNetoG.multiply(precioPorGramoPen).setScale(2, RoundingMode.HALF_UP);
    }
}
