package pe.edu.upeu.sitraoro.acopio.prestamo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sitraoro.CentroActual;
import pe.edu.upeu.sitraoro.ConflictoNegocioException;
import pe.edu.upeu.sitraoro.RecursoNoEncontradoException;
import pe.edu.upeu.sitraoro.acopio.caja.entity.OrigenCajaAcopio;
import pe.edu.upeu.sitraoro.acopio.caja.service.CajaAcopioService;
import pe.edu.upeu.sitraoro.acopio.minero.entity.Minero;
import pe.edu.upeu.sitraoro.acopio.minero.service.MineroService;
import pe.edu.upeu.sitraoro.acopio.prestamo.dto.PrestamoRequest;
import pe.edu.upeu.sitraoro.acopio.prestamo.dto.PrestamoResponse;
import pe.edu.upeu.sitraoro.acopio.prestamo.entity.EstadoPrestamo;
import pe.edu.upeu.sitraoro.acopio.prestamo.entity.PrestamoMinero;
import pe.edu.upeu.sitraoro.acopio.prestamo.repository.PrestamoMineroRepository;

/**
 * Adelantos del acopiador a sus mineros. Quedan PENDIENTES hasta descontarse en una compra
 * al mismo minero.
 */
@Service
@RequiredArgsConstructor
public class PrestamoMineroService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final PrestamoMineroRepository repository;
    private final MineroService mineros;
    private final CajaAcopioService caja;
    private final CentroActual centro;

    @Transactional
    public PrestamoResponse registrar(PrestamoRequest request) {
        Minero minero = mineros.obtenerEntidad(request.idMinero());
        String obs = request.observacion() == null || request.observacion().isBlank()
                ? null : request.observacion().trim();
        PrestamoMinero p = new PrestamoMinero();
        p.setIdCentroAcopio(centro.id());
        p.setMinero(minero);
        p.setFecha(request.fecha() == null ? LocalDate.now(LIMA) : request.fecha());
        p.setMontoPen(request.montoPen().setScale(2, RoundingMode.HALF_UP));
        p.setObservacion(obs);
        p.setEstado(EstadoPrestamo.PENDIENTE);
        PrestamoMinero guardado = repository.saveAndFlush(p);
        // El adelanto sale de la caja el dia que se entrega (falla si no alcanza el saldo)
        caja.registrarPrestamo(guardado.getIdPrestamo(), guardado.getMontoPen(),
                minero.getNombresApellidos(), guardado.getFecha());
        return respuesta(guardado);
    }

    @Transactional(readOnly = true)
    public List<PrestamoResponse> listar(Long idMinero, EstadoPrestamo estado) {
        return repository.buscar(centro.id(), idMinero, estado).stream().map(PrestamoMineroService::respuesta).toList();
    }

    @Transactional
    public PrestamoResponse anular(Long id) {
        PrestamoMinero p = repository.bloquear(List.of(id)).stream()
                .filter(x -> Objects.equals(x.getIdCentroAcopio(), centro.id())).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Adelanto no encontrado: " + id));
        if (p.getEstado() != EstadoPrestamo.PENDIENTE) {
            throw new ConflictoNegocioException("Solo se anula un adelanto pendiente; este ya esta " + p.getEstado());
        }
        p.setEstado(EstadoPrestamo.ANULADO);
        caja.anularAutomatico(OrigenCajaAcopio.PRESTAMO, id);
        return respuesta(p);
    }

    @Transactional(readOnly = true)
    public List<Long> idsDescontados(Long idCompra) {
        return repository.findByIdCompraG2(idCompra).stream().map(PrestamoMinero::getIdPrestamo).sorted().toList();
    }

    /**
     * Descuenta los adelantos elegidos en la compra y devuelve su suma. Valida que sean del mismo minero
     * y que sigan pendientes.
     */
    @Transactional
    public BigDecimal descontar(List<Long> ids, Long idMinero, Long idCompra) {
        if (ids == null || ids.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Un adelanto no puede repetirse");
        }
        List<PrestamoMinero> elegidos = repository.bloquear(ids);
        if (elegidos.size() != ids.size()) {
            throw new RecursoNoEncontradoException("Uno de los adelantos seleccionados no existe");
        }
        BigDecimal suma = BigDecimal.ZERO;
        for (PrestamoMinero p : elegidos) {
            if (!Objects.equals(p.getMinero().getIdMinero(), idMinero)) {
                throw new IllegalArgumentException("El adelanto #" + p.getIdPrestamo() + " no es de este minero");
            }
            if (p.getEstado() != EstadoPrestamo.PENDIENTE) {
                throw new ConflictoNegocioException("El adelanto #" + p.getIdPrestamo() + " ya no esta pendiente");
            }
            suma = suma.add(p.getMontoPen());
        }
        for (PrestamoMinero p : elegidos) {
            p.setEstado(EstadoPrestamo.DESCONTADO);
            p.setIdCompraG2(idCompra);
        }
        return suma;
    }

    private static PrestamoResponse respuesta(PrestamoMinero p) {
        Minero m = p.getMinero();
        return new PrestamoResponse(p.getIdPrestamo(), m.getIdMinero(), m.getNombresApellidos(),
                m.getDocumentoIdentidad(), p.getFecha(), p.getMontoPen(), p.getObservacion(),
                p.getEstado().name(), p.getIdCompraG2());
    }
}
