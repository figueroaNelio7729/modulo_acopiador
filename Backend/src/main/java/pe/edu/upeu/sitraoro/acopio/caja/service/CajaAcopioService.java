package pe.edu.upeu.sitraoro.acopio.caja.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sitraoro.CentroActual;
import pe.edu.upeu.sitraoro.ConflictoNegocioException;
import pe.edu.upeu.sitraoro.RecursoNoEncontradoException;
import pe.edu.upeu.sitraoro.acopio.caja.dto.MovimientoManualRequest;
import pe.edu.upeu.sitraoro.acopio.caja.dto.MovimientoResponse;
import pe.edu.upeu.sitraoro.acopio.caja.dto.ResumenCaja;
import pe.edu.upeu.sitraoro.acopio.caja.entity.MovimientoCajaAcopio;
import pe.edu.upeu.sitraoro.acopio.caja.entity.OrigenCajaAcopio;
import pe.edu.upeu.sitraoro.acopio.caja.repository.MovimientoCajaAcopioRepository;

/**
 * Caja del centro, solo en soles. Los pagos a mineros y los adelantos se registran solos;
 * el saldo inicial, lo que paga el mayorista y otros movimientos los registra el acopiador.
 */
@Service
@RequiredArgsConstructor
public class CajaAcopioService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final MovimientoCajaAcopioRepository repository;
    private final CentroActual centro;

    @Transactional(readOnly = true)
    public ResumenCaja resumen() {
        LocalDate hoy = LocalDate.now(LIMA);
        List<MovimientoCajaAcopio> mes = repository.buscar(centro.id(), hoy.withDayOfMonth(1), hoy).stream()
                .filter(m -> !m.isAnulado()).toList();
        BigDecimal ingresos = mes.stream().filter(MovimientoCajaAcopio::isIngreso)
                .map(MovimientoCajaAcopio::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal egresos = mes.stream().filter(m -> !m.isIngreso())
                .map(MovimientoCajaAcopio::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<MovimientoResponse> ultimos = repository.buscar(centro.id(), null, null).stream().limit(15)
                .map(CajaAcopioService::respuesta).toList();
        return new ResumenCaja(saldo(), ingresos, egresos, ultimos);
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> listar(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede superar la fecha final");
        }
        return repository.buscar(centro.id(), desde, hasta).stream().map(CajaAcopioService::respuesta).toList();
    }

    @Transactional
    public MovimientoResponse registrarManual(MovimientoManualRequest request) {
        boolean ingreso = "INGRESO".equals(request.tipo());
        boolean saldoInicial = Boolean.TRUE.equals(request.saldoInicial());
        if (saldoInicial && !ingreso) {
            throw new IllegalArgumentException("El saldo inicial se registra como ingreso");
        }
        OrigenCajaAcopio origen = saldoInicial ? OrigenCajaAcopio.SALDO_INICIAL : OrigenCajaAcopio.MANUAL;
        return respuesta(guardar(ingreso, origen, request.monto(), request.concepto(), null, request.fecha()));
    }

    @Transactional
    public MovimientoResponse anular(Long id) {
        MovimientoCajaAcopio m = repository.findByIdMovimientoAndIdCentroAcopio(id, centro.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("Movimiento de caja no encontrado: " + id));
        if (!m.getOrigen().esManual()) {
            throw new ConflictoNegocioException(
                    "Este movimiento lo genera el sistema; se corrige desde la compra o el adelanto");
        }
        if (m.isAnulado()) {
            throw new ConflictoNegocioException("El movimiento ya esta anulado");
        }
        m.setAnulado(true);
        return respuesta(m);
    }

    // ---- Movimientos automaticos ----------------------------------------------------------

    /** Egreso por lo que se entrego al minero en una compra (neto, en soles). */
    @Transactional
    public void registrarPagoCompra(Long idCompra, BigDecimal neto, String minero) {
        if (neto == null || neto.signum() <= 0) {
            return;
        }
        guardar(false, OrigenCajaAcopio.PAGO_COMPRA, neto, "Compra #" + idCompra + " · " + minero, idCompra, null);
    }

    /** Egreso por un adelanto. Nunca se entrega mas dinero del que hay en caja. */
    @Transactional
    public void registrarPrestamo(Long idPrestamo, BigDecimal monto, String minero, LocalDate fecha) {
        BigDecimal disponible = saldo();
        if (disponible.compareTo(monto) < 0) {
            throw new ConflictoNegocioException("No hay suficiente dinero en caja: tienes S/ "
                    + disponible.setScale(2, RoundingMode.HALF_UP).toPlainString()
                    + " y el adelanto es de S/ " + monto.setScale(2, RoundingMode.HALF_UP).toPlainString()
                    + ". Registra primero el ingreso en la caja.");
        }
        guardar(false, OrigenCajaAcopio.PRESTAMO, monto, "Adelanto #" + idPrestamo + " · " + minero, idPrestamo, fecha);
    }

    @Transactional
    public void anularAutomatico(OrigenCajaAcopio origen, Long idReferencia) {
        repository.findByOrigenAndIdReferenciaAndAnuladoFalse(origen, idReferencia).forEach(m -> m.setAnulado(true));
    }

    private BigDecimal saldo() {
        BigDecimal s = repository.saldo(centro.id());
        return s == null ? BigDecimal.ZERO : s;
    }

    private MovimientoCajaAcopio guardar(boolean ingreso, OrigenCajaAcopio origen, BigDecimal monto,
            String concepto, Long idReferencia, LocalDate fecha) {
        String texto = concepto == null ? "" : concepto.trim();
        MovimientoCajaAcopio m = new MovimientoCajaAcopio();
        m.setIdCentroAcopio(centro.id());
        m.setFecha(fecha == null ? LocalDate.now(LIMA) : fecha);
        m.setMoneda("PEN");
        m.setIngreso(ingreso);
        m.setOrigen(origen);
        m.setMonto(monto.setScale(2, RoundingMode.HALF_UP));
        m.setConcepto(texto.length() > 200 ? texto.substring(0, 200) : texto);
        m.setIdReferencia(idReferencia);
        m.setAnulado(false);
        return repository.save(m);
    }

    private static MovimientoResponse respuesta(MovimientoCajaAcopio m) {
        return new MovimientoResponse(m.getIdMovimiento(), m.getFecha(), m.isIngreso() ? "INGRESO" : "EGRESO",
                m.getOrigen().name(), m.getMonto(), m.getConcepto(), m.getIdReferencia(), m.isAnulado(),
                m.getOrigen().esManual() && !m.isAnulado());
    }
}
