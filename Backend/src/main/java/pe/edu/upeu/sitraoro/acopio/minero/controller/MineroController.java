package pe.edu.upeu.sitraoro.acopio.minero.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sitraoro.acopio.minero.dto.MineroRequest;
import pe.edu.upeu.sitraoro.acopio.minero.dto.MineroResponse;
import pe.edu.upeu.sitraoro.acopio.minero.service.MineroService;

@RestController
@RequestMapping("/api/v1/mineros")
@RequiredArgsConstructor
@Tag(name = "1. Mineros", description = "Registro y consulta de mineros artesanales")
public class MineroController {

    private final MineroService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un nuevo minero")
    public MineroResponse crear(@Valid @RequestBody MineroRequest request) {
        return service.crear(request);
    }

    @GetMapping
    @Operation(summary = "Lista todos los mineros registrados")
    public List<MineroResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un minero por ID")
    public MineroResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza los datos de un minero")
    public MineroResponse actualizar(@PathVariable Long id, @Valid @RequestBody MineroRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un minero (409 si ya tiene movimientos)")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
