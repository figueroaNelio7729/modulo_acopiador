package pe.edu.upeu.sitraoro.acopio.minero.controller;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

/** Consulta el nombre completo de una persona por DNI en RENIEC (API Codart). */
@RestController
@RequestMapping("/api/v1/publico/identidad")
@Tag(name = "1. Mineros")
public class ConsultaDniController {

    public record Resultado(String nombresApellidos) {
    }

    private record Ventana(long inicio, int consultas) {
    }

    private final RestClient cliente;
    private final String token;
    private final ConcurrentHashMap<String, Ventana> limites = new ConcurrentHashMap<>();

    public ConsultaDniController(@Value("${sitra.identidad.codart-token:}") String token) {
        this.token = token;
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.cliente = RestClient.builder().requestFactory(factory).baseUrl("https://api-codart.cgrt.org").build();
    }

    @GetMapping("/dni/{dni}")
    @Operation(summary = "Obtiene nombres y apellidos desde RENIEC a partir del DNI")
    public Resultado consultar(@PathVariable String dni, HttpServletRequest request) {
        if (!dni.matches("\\d{8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa un DNI de 8 dígitos");
        }
        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Consulta de DNI no configurada");
        }
        // Limite simple: 10 consultas por minuto desde la misma IP
        long ahora = System.currentTimeMillis();
        limites.entrySet().removeIf(e -> ahora - e.getValue().inicio() >= 60_000);
        Ventana ventana = limites.compute(request.getRemoteAddr(), (ip, anterior) -> anterior == null
                ? new Ventana(ahora, 1)
                : new Ventana(anterior.inicio(), anterior.consultas() + 1));
        if (ventana.consultas() > 10) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Espera un minuto antes de volver a consultar");
        }
        try {
            Map<?, ?> respuesta = cliente.get().uri("/api/v1/consultas/reniec/dni/{dni}", dni)
                    .headers(h -> {
                        h.setBearerAuth(token);
                        h.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .retrieve().body(Map.class);
            if (respuesta == null || !Boolean.TRUE.equals(respuesta.get("success"))
                    || !(respuesta.get("result") instanceof Map<?, ?> datos)) {
                throw new IllegalStateException();
            }
            String nombre = String.join(" ",
                    campo(datos, "first_name"),
                    campo(datos, "apellidoPaterno", "apellido_paterno", "first_last_name"),
                    campo(datos, "apellidoMaterno", "apellido_materno", "second_last_name"))
                    .trim().replaceAll("\\s+", " ");
            if (nombre.isBlank()) {
                nombre = campo(datos, "full_name");
            }
            if (nombre.isBlank()) {
                throw new IllegalStateException();
            }
            return new Resultado(nombre);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar el DNI. Ingresa los nombres manualmente");
        }
    }

    private static String campo(Map<?, ?> datos, String... claves) {
        for (String clave : claves) {
            if (datos.get(clave) instanceof String valor && !valor.isBlank()) {
                return valor.trim();
            }
        }
        return "";
    }
}
