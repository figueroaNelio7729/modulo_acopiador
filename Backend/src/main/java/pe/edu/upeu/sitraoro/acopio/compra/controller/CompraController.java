package pe.edu.upeu.sitraoro.acopio.compra.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sitraoro.acopio.compra.dto.CompraRequest;
import pe.edu.upeu.sitraoro.acopio.compra.dto.CompraResponse;
import pe.edu.upeu.sitraoro.acopio.compra.dto.PrecioOroResponse;
import pe.edu.upeu.sitraoro.acopio.compra.service.CalculadorPrecioOro;
import pe.edu.upeu.sitraoro.acopio.compra.service.CompraService;

@RestController
@RequestMapping({ "/api/v1/acopio/compras", "/api/v1/compras" })
@RequiredArgsConstructor
@Tag(name = "2. Compras de Oro", description = "Calculo de precio y compra multipartida a mineros")
public class CompraController {

    private final CompraService compraService;
    private final CalculadorPrecioOro calculador;

    @GetMapping("/precio-oro")
    @Operation(summary = "Calcula el precio por gramo de Oro Rojo (90%) o Verde (80%)")
    public PrecioOroResponse calcularPrecio(
            @RequestParam String tipoOro,
            @RequestParam BigDecimal onzaUsd,
            @RequestParam BigDecimal tipoCambioPen,
            @RequestParam(defaultValue = "5") BigDecimal margenExportacionPct) {
        if (!"ROJO".equalsIgnoreCase(tipoOro) && !"VERDE".equalsIgnoreCase(tipoOro)) {
            throw new IllegalArgumentException("tipoOro debe ser ROJO o VERDE");
        }
        BigDecimal precio = calculador.calcularPrecioGramoPen(tipoOro, onzaUsd, tipoCambioPen, margenExportacionPct);
        return new PrecioOroResponse(tipoOro.toUpperCase(), "ROJO".equalsIgnoreCase(tipoOro) ? 90 : 80, precio);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una compra con una o varias partidas de un mismo minero")
    public CompraResponse registrar(@Valid @RequestBody CompraRequest request) {
        return compraService.registrarCompra(request);
    }

    @GetMapping
    @Operation(summary = "Lista las compras con sus partidas y mermas")
    public List<CompraResponse> listar() {
        return compraService.listarCompras();
    }
}
