package pe.edu.upeu.sitraoro.acopio.cotizador;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Precio por gramo vigente de cada color, tal como lo publica el sistema (tabla COTIZACIONES_COLOR). */
@RestController
@RequestMapping("/api/v1/cotizador")
@RequiredArgsConstructor
@Tag(name = "2. Compras de Oro")
public class CotizadorController {

    public record CotizacionColor(String color, BigDecimal precioGramoPen, BigDecimal onzaUsd,
            BigDecimal dolarPen, BigDecimal exportacionPct, BigDecimal leyPct, Timestamp fechaPublicacion) {
    }

    private final JdbcTemplate jdbc;

    @GetMapping("/colores")
    @Operation(summary = "Ultimo precio por gramo publicado para ROJO y VERDE")
    public List<CotizacionColor> colores() {
        return jdbc.query("""
                SELECT c.COLOR, c.PRECIO_GRAMO_PEN, c.ONZA_USD, c.DOLAR_PEN, c.EXPORTACION_PCT, c.LEY_PCT,
                       c.FECHA_PUBLICACION
                FROM COTIZACIONES_COLOR c
                WHERE c.ID_COTIZACION = (SELECT MAX(x.ID_COTIZACION) FROM COTIZACIONES_COLOR x WHERE x.COLOR = c.COLOR)
                ORDER BY c.COLOR
                """, (rs, i) -> new CotizacionColor(rs.getString("COLOR"), rs.getBigDecimal("PRECIO_GRAMO_PEN"),
                rs.getBigDecimal("ONZA_USD"), rs.getBigDecimal("DOLAR_PEN"), rs.getBigDecimal("EXPORTACION_PCT"),
                rs.getBigDecimal("LEY_PCT"), rs.getTimestamp("FECHA_PUBLICACION")));
    }
}
