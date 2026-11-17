package pe.edu.upeu.sitraoro;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Contrato de errores: 400 datos invalidos, 404 no existe, 409 conflicto, 500 tecnico sin secretos. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(f -> campos.put(f.getField(), f.getDefaultMessage()));
        Map<String, Object> cuerpo = cuerpo(HttpStatus.BAD_REQUEST, "Datos invalidos");
        cuerpo.put("campos", campos);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es valido"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(ConflictoNegocioException.class)
    public ResponseEntity<Map<String, Object>> conflicto(ConflictoNegocioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> estado(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status).body(cuerpo(status, ex.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(cuerpo(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor"));
    }

    private Map<String, Object> cuerpo(HttpStatus status, String mensaje) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", LocalDateTime.now().toString());
        m.put("status", status.value());
        m.put("error", status.getReasonPhrase());
        m.put("mensaje", mensaje);
        return m;
    }
}
