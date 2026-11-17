package pe.edu.upeu.sitraoro.acopio.entrega.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sitraoro.acopio.entrega.dto.DisponibleResponse;
import pe.edu.upeu.sitraoro.acopio.entrega.dto.EntregaRequest;
import pe.edu.upeu.sitraoro.acopio.entrega.dto.EntregaResponse;
import pe.edu.upeu.sitraoro.acopio.entrega.dto.StockPendienteResponse;
import pe.edu.upeu.sitraoro.acopio.entrega.service.EntregaMayoristaService;

@RestController
@RequestMapping("/api/v1/acopio/entregas-mayorista")
@RequiredArgsConstructor
@Tag(name = "4. Entrega al mayorista")
public class EntregaMayoristaController {

    private final EntregaMayoristaService service;

    @GetMapping("/disponibles")
    @Operation(summary = "Compras (partidas) todavia disponibles para enviar al mayorista")
    public List<DisponibleResponse> disponibles() {
        return service.disponibles();
    }

    @GetMapping("/stock")
    @Operation(summary = "Stock pendiente de envio por color (gramos y soles)")
    public StockPendienteResponse stock() {
        return service.stock();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envia las compras seleccionadas al mayorista y las marca como entregadas")
    public EntregaResponse registrar(@Valid @RequestBody EntregaRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    @Operation(summary = "Reporte de entregas enviadas al mayorista")
    public List<EntregaResponse> reporte() {
        return service.reporte();
    }
}
