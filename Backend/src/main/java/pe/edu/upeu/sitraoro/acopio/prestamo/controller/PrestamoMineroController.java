package pe.edu.upeu.sitraoro.acopio.prestamo.controller;

import java.util.List;

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
import pe.edu.upeu.sitraoro.acopio.prestamo.dto.PrestamoRequest;
import pe.edu.upeu.sitraoro.acopio.prestamo.dto.PrestamoResponse;
import pe.edu.upeu.sitraoro.acopio.prestamo.entity.EstadoPrestamo;
import pe.edu.upeu.sitraoro.acopio.prestamo.service.PrestamoMineroService;

@RestController
@RequestMapping({ "/api/v1/acopio/prestamos", "/api/v1/prestamos" })
@RequiredArgsConstructor
@Tag(name = "3. Caja y adelantos")
public class PrestamoMineroController {

    private final PrestamoMineroService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un adelanto en soles a un minero (sale de la caja)")
    public PrestamoResponse registrar(@Valid @RequestBody PrestamoRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    @Operation(summary = "Adelantos del centro, opcionalmente por minero y estado (PENDIENTE, DESCONTADO, ANULADO)")
    public List<PrestamoResponse> listar(@RequestParam(required = false) Long idMinero,
            @RequestParam(required = false) EstadoPrestamo estado) {
        return service.listar(idMinero, estado);
    }

    @PatchMapping("/{id}/anular")
    @Operation(summary = "Anula un adelanto pendiente registrado por error")
    public PrestamoResponse anular(@PathVariable Long id) {
        return service.anular(id);
    }
}
