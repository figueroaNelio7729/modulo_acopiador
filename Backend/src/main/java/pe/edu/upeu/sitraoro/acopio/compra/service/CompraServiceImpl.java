package pe.edu.upeu.sitraoro.acopio.compra.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sitraoro.CentroActual;
import pe.edu.upeu.sitraoro.acopio.caja.service.CajaAcopioService;
import pe.edu.upeu.sitraoro.acopio.compra.dto.CompraRequest;
import pe.edu.upeu.sitraoro.acopio.compra.dto.CompraResponse;
import pe.edu.upeu.sitraoro.acopio.compra.dto.PartidaRequest;
import pe.edu.upeu.sitraoro.acopio.compra.dto.PartidaResponse;
import pe.edu.upeu.sitraoro.acopio.compra.entity.CompraG2;
import pe.edu.upeu.sitraoro.acopio.compra.entity.TransaccionG2;
import pe.edu.upeu.sitraoro.acopio.compra.repository.CompraG2Repository;
import pe.edu.upeu.sitraoro.acopio.minero.entity.Minero;
import pe.edu.upeu.sitraoro.acopio.minero.service.MineroService;
import pe.edu.upeu.sitraoro.acopio.prestamo.service.PrestamoMineroService;

@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements CompraService {

    private final CompraG2Repository compraRepository;
    private final MineroService mineroService; // usa el servicio publico del otro submodulo, no su repository
    private final PrestamoMineroService prestamos;
    private final CajaAcopioService caja;
    private final CalculadorPrecioOro calculador;
    private final CentroActual centro;

    @Override
    @Transactional
    public CompraResponse registrarCompra(CompraRequest request) {
        Minero minero = mineroService.obtenerEntidad(request.idMinero());

        CompraG2 compra = new CompraG2();
        compra.setMinero(minero);
        compra.setIdCentroAcopio(centro.id());

        for (PartidaRequest item : request.partidas()) {
            calculador.calcularMermaGramos(item.pesoSinFundirG(), item.pesoFundidoNetoG());
            BigDecimal importe = calculador.calcularImportePartida(item.pesoFundidoNetoG(), item.precioGramoPen());

            TransaccionG2 partida = new TransaccionG2();
            partida.setMinero(minero);
            partida.setIdCentroAcopio(centro.id());
            partida.setTipoOro(item.tipoOro());
            partida.setPesoSinFundirG(item.pesoSinFundirG());
            partida.setPesoFundidoNetoG(item.pesoFundidoNetoG());
            partida.setPrecioAplicadoPen(item.precioGramoPen());
            partida.setTotalPagadoPen(importe);
            compra.agregarPartida(partida);
        }

        CompraG2 guardada = compraRepository.saveAndFlush(compra);

        // Liquidacion: valor del oro - adelantos - descuento manual (nunca mas que el oro)
        BigDecimal material = guardada.getDetalles().stream().map(TransaccionG2::getTotalPagadoPen)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal manual = request.descuentoManualPen() == null
                ? BigDecimal.ZERO : request.descuentoManualPen().setScale(2, RoundingMode.HALF_UP);
        String motivo = request.motivoDescuento() == null || request.motivoDescuento().isBlank()
                ? null : request.motivoDescuento().trim();
        if (manual.signum() > 0 && motivo == null) {
            throw new IllegalArgumentException(
                    "Escribe el motivo del descuento (por ejemplo: deuda de viveres, herramientas)");
        }
        BigDecimal descuento = prestamos
                .descontar(request.idsPrestamos(), minero.getIdMinero(), guardada.getIdCompraG2())
                .add(manual);
        if (descuento.compareTo(material) > 0) {
            throw new IllegalArgumentException("Los adelantos y descuentos (S/ " + descuento.toPlainString()
                    + ") superan el valor del oro (S/ " + material.toPlainString() + ")");
        }
        guardada.setDescuentoPen(descuento);
        guardada.setDescuentoManualPen(manual);
        guardada.setDescuentoMotivo(manual.signum() > 0 ? motivo : null);

        // Lo que se entrega al minero sale de la caja (el adelanto ya salio cuando se entrego)
        caja.registrarPagoCompra(guardada.getIdCompraG2(), material.subtract(descuento), minero.getNombresApellidos());

        return toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraResponse> listarCompras() {
        return compraRepository.findByIdCentroAcopioOrderByFechaCompraDescIdCompraG2Desc(centro.id())
                .stream().map(this::toResponse).toList();
    }

    private CompraResponse toResponse(CompraG2 c) {
        List<PartidaResponse> partidas = c.getDetalles().stream().map(d -> new PartidaResponse(
                d.getIdTransaccionG2(),
                d.getTipoOro(),
                d.getPesoSinFundirG(),
                d.getPesoFundidoNetoG(),
                calculador.calcularMermaGramos(d.getPesoSinFundirG(), d.getPesoFundidoNetoG()),
                d.getPrecioAplicadoPen(),
                d.getTotalPagadoPen(),
                d.getIdRecepcionMayorista())).toList();

        BigDecimal total = partidas.stream().map(PartidaResponse::totalPagadoPen)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descuento = c.getDescuentoPen() == null ? BigDecimal.ZERO : c.getDescuentoPen();
        BigDecimal manual = c.getDescuentoManualPen() == null ? BigDecimal.ZERO : c.getDescuentoManualPen();

        return new CompraResponse(c.getIdCompraG2(), c.getMinero().getIdMinero(),
                c.getMinero().getNombresApellidos(), c.getMinero().getDocumentoIdentidad(),
                c.getFechaCompra(), total, partidas, descuento, manual,
                total.subtract(descuento).max(BigDecimal.ZERO),
                prestamos.idsDescontados(c.getIdCompraG2()), c.getDescuentoMotivo());
    }
}
