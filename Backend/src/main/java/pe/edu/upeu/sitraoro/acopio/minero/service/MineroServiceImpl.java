package pe.edu.upeu.sitraoro.acopio.minero.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sitraoro.CentroActual;
import pe.edu.upeu.sitraoro.ConflictoNegocioException;
import pe.edu.upeu.sitraoro.RecursoNoEncontradoException;
import pe.edu.upeu.sitraoro.acopio.minero.dto.MineroRequest;
import pe.edu.upeu.sitraoro.acopio.minero.dto.MineroResponse;
import pe.edu.upeu.sitraoro.acopio.minero.entity.Minero;
import pe.edu.upeu.sitraoro.acopio.minero.repository.MineroRepository;

@Service
@RequiredArgsConstructor
public class MineroServiceImpl implements MineroService {

    private final MineroRepository repository;
    private final CentroActual centro;

    @Override
    @Transactional
    public MineroResponse crear(MineroRequest request) {
        if (repository.existsByDocumentoIdentidad(request.documentoIdentidad())) {
            throw new ConflictoNegocioException(
                    "Ya existe un minero con el documento: " + request.documentoIdentidad());
        }
        Minero m = new Minero();
        m.setIdCentroAcopio(centro.id());
        aplicar(m, request);
        return toResponse(repository.save(m));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MineroResponse> listar() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MineroResponse obtener(Long idMinero) {
        return toResponse(obtenerEntidad(idMinero));
    }

    @Override
    @Transactional
    public MineroResponse actualizar(Long idMinero, MineroRequest request) {
        Minero m = obtenerEntidad(idMinero);
        if (repository.existsByDocumentoIdentidadAndIdMineroNot(request.documentoIdentidad(), idMinero)) {
            throw new ConflictoNegocioException(
                    "Ya existe otro minero con el documento: " + request.documentoIdentidad());
        }
        aplicar(m, request);
        return toResponse(m);
    }

    @Override
    @Transactional
    public void eliminar(Long idMinero) {
        Minero m = obtenerEntidad(idMinero);
        try {
            repository.delete(m);
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoNegocioException(
                    "No se puede eliminar: el minero ya tiene compras o adelantos registrados");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Minero obtenerEntidad(Long idMinero) {
        return repository.findById(idMinero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Minero no encontrado con ID: " + idMinero));
    }

    private void aplicar(Minero m, MineroRequest request) {
        m.setDocumentoIdentidad(request.documentoIdentidad());
        m.setNombresApellidos(request.nombresApellidos());
        m.setTelefono(request.telefono());
        m.setZonaProcedencia(request.zonaProcedencia());
    }

    private MineroResponse toResponse(Minero m) {
        return new MineroResponse(m.getIdMinero(), m.getDocumentoIdentidad(), m.getNombresApellidos(),
                m.getTelefono(), m.getZonaProcedencia(), m.getFechaRegistro());
    }
}
