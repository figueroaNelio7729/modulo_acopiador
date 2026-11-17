package pe.edu.upeu.sitraoro.acopio.caja.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import pe.edu.upeu.sitraoro.acopio.caja.dto.MovimientoManualRequest;
import pe.edu.upeu.sitraoro.acopio.caja.dto.MovimientoResponse;
import pe.edu.upeu.sitraoro.acopio.caja.dto.ResumenCaja;
import pe.edu.upeu.sitraoro.acopio.caja.service.CajaAcopioService;

@RestController
@RequestMapping("/api/v1/acopio/caja")
@RequiredArgsConstructor
@Tag(name = "3. Caja y adelantos")
public class CajaAcopioController {

    private final CajaAcopioService service;

    @GetMapping
    @Operation(summary = "Saldo de caja en soles, totales del mes y ultimos movimientos")
    public ResumenCaja resumen() {
        return service.resumen();
    }

    @GetMapping("/movimientos")
    @Operation(summary = "Movimientos de caja, opcionalmente por rango de fechas")
    public List<MovimientoResponse> movimientos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listar(desde, hasta);
    }

    @PostMapping("/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un ingreso o egreso manual, o el saldo inicial")
    public MovimientoResponse registrar(@Valid @RequestBody MovimientoManualRequest request) {
        return service.registrarManual(request);
    }

    @PatchMapping("/movimientos/{id}/anular")
    @Operation(summary = "Anula un movimiento manual registrado por error")
    public MovimientoResponse anular(@PathVariable Long id) {
        return service.anular(id);
    }
}
