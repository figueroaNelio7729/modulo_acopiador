package pe.edu.upeu.sitraoro.acopio.compra.service;

import java.util.List;

import pe.edu.upeu.sitraoro.acopio.compra.dto.CompraRequest;
import pe.edu.upeu.sitraoro.acopio.compra.dto.CompraResponse;

public interface CompraService {
    CompraResponse registrarCompra(CompraRequest request);

    List<CompraResponse> listarCompras();
}
