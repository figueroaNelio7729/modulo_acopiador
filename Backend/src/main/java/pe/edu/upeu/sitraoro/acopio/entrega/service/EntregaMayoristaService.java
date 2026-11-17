package pe.edu.upeu.sitraoro.acopio.entrega.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sitraoro.CentroActual;
import pe.edu.upeu.sitraoro.ConflictoNegocioException;
import pe.edu.upeu.sitraoro.RecursoNoEncontradoException;
import pe.edu.upeu.sitraoro.acopio.compra.entity.TransaccionG2;
import pe.edu.upeu.sitraoro.acopio.compra.repository.TransaccionG2Repository;
import pe.edu.upeu.sitraoro.acopio.entrega.dto.DisponibleResponse;
import pe.edu.upeu.sitraoro.acopio.entrega.dto.EntregaRequest;
import pe.edu.upeu.sitraoro.acopio.entrega.dto.EntregaResponse;
import pe.edu.upeu.sitraoro.acopio.entrega.dto.StockPendienteResponse;

/**
 * Entrega del oro comprado al mayorista. Crea la recepcion en MAYORISTA_RECEPCIONES y marca cada partida
 * con ID_RECEPCION_MAYORISTA: asi sale de "compras disponibles" y del stock pendiente.
 */
@Service
@RequiredArgsConstructor
public class EntregaMayoristaService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final TransaccionG2Repository transacciones;
    private final JdbcTemplate jdbc;
    private final CentroActual centro;

    @Transactional(readOnly = true)
    public List<DisponibleResponse> disponibles() {
        return transacciones.disponibles(centro.id()).stream().map(t -> new DisponibleResponse(
                t.getIdTransaccionG2(),
                t.getCompra() == null ? null : t.getCompra().getIdCompraG2(),
                t.getTipoOro(), t.getMinero().getIdMinero(), t.getMinero().getNombresApellidos(),
                t.getMinero().getDocumentoIdentidad(), t.getPesoSinFundirG(), t.getPesoFundidoNetoG(),
                t.getPrecioAplicadoPen(), t.getTotalPagadoPen(), t.getFechaTransaccion())).toList();
    }

    @Transactional(readOnly = true)
    public StockPendienteResponse stock() {
        List<TransaccionG2> lista = transacciones.disponibles(centro.id());
        return new StockPendienteResponse(
                sumar(lista, "ROJO", TransaccionG2::getPesoFundidoNetoG),
                sumar(lista, "ROJO", TransaccionG2::getTotalPagadoPen),
                sumar(lista, "VERDE", TransaccionG2::getPesoFundidoNetoG),
                sumar(lista, "VERDE", TransaccionG2::getTotalPagadoPen));
    }

    @Transactional
    public EntregaResponse registrar(EntregaRequest request) {
        List<Long> ids = request.idsComprasAcopiador();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Una compra no puede repetirse en la entrega");
        }
        List<TransaccionG2> compras = transacciones.bloquearPorIds(ids);
        if (compras.size() != ids.size()) {
            throw new RecursoNoEncontradoException("Una de las compras seleccionadas ya no existe");
        }
        for (TransaccionG2 t : compras) {
            if (!centro.id().equals(t.getIdCentroAcopio())) {
                throw new ConflictoNegocioException("La compra " + t.getIdTransaccionG2() + " pertenece a otro centro");
            }
            if (t.getAnulada() != null && t.getAnulada() == 1) {
                throw new ConflictoNegocioException("La compra " + t.getIdTransaccionG2() + " esta anulada");
            }
            if (t.getIdRecepcionMayorista() != null) {
                throw new ConflictoNegocioException("La compra " + t.getIdTransaccionG2() + " ya fue enviada al mayorista");
            }
            if (t.getIdLiquidacionG1() != null) {
                throw new ConflictoNegocioException("La compra " + t.getIdTransaccionG2() + " ya esta liquidada");
            }
        }
        // Lo fundido por el acopiador es el peso de entrada del mayorista (antes de su propia fundicion)
        BigDecimal rojoNeto = sumar(compras, "ROJO", TransaccionG2::getPesoFundidoNetoG);
        BigDecimal verdeNeto = sumar(compras, "VERDE", TransaccionG2::getPesoFundidoNetoG);
        boolean hayRojo = compras.stream().anyMatch(t -> "ROJO".equals(t.getTipoOro()));
        boolean hayVerde = compras.stream().anyMatch(t -> "VERDE".equals(t.getTipoOro()));

        LocalDate hoy = LocalDate.now(LIMA);
        String nombre = jdbc.queryForObject(
                "SELECT NOMBRE || ' · ' || ZONA FROM CENTROS_ACOPIO WHERE ID_CENTRO_ACOPIO = ?",
                String.class, centro.id());

        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("""
                    INSERT INTO MAYORISTA_RECEPCIONES
                      (FECHA, ID_CENTRO_ACOPIO, NOMBRE_ACOPIADOR,
                       ROJO_PESO_SIN_FUNDIR_G, VERDE_PESO_SIN_FUNDIR_G)
                    VALUES (?, ?, ?, ?, ?)
                    """, new String[] { "ID_RECEPCION" });
            ps.setDate(1, Date.valueOf(hoy));
            ps.setLong(2, centro.id());
            ps.setString(3, nombre);
            ps.setBigDecimal(4, hayRojo ? rojoNeto : null);
            ps.setBigDecimal(5, hayVerde ? verdeNeto : null);
            return ps;
        }, key);
        Long idRecepcion = key.getKey().longValue();

        compras.forEach(t -> t.setIdRecepcionMayorista(idRecepcion));
        transacciones.saveAll(compras);

        return new EntregaResponse(idRecepcion, hoy, nombre, rojoNeto, verdeNeto, rojoNeto.add(verdeNeto),
                compras.stream().map(TransaccionG2::getIdTransaccionG2).sorted().toList());
    }

    /** Historial de entregas enviadas al mayorista por este centro. */
    @Transactional(readOnly = true)
    public List<EntregaResponse> reporte() {
        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT ID_RECEPCION, FECHA, NOMBRE_ACOPIADOR, ROJO_PESO_SIN_FUNDIR_G, VERDE_PESO_SIN_FUNDIR_G
                FROM MAYORISTA_RECEPCIONES
                WHERE ID_CENTRO_ACOPIO = ? AND COMPRA_DIRECTA = 0
                ORDER BY ID_RECEPCION DESC FETCH FIRST 100 ROWS ONLY
                """, centro.id());
        List<Long> idsRecepcion = filas.stream().map(f -> ((Number) f.get("ID_RECEPCION")).longValue()).toList();
        Map<Long, List<Long>> porRecepcion = idsRecepcion.isEmpty() ? Map.of()
                : transacciones.findByIdRecepcionMayoristaIn(idsRecepcion).stream().collect(
                        Collectors.groupingBy(TransaccionG2::getIdRecepcionMayorista,
                                Collectors.mapping(TransaccionG2::getIdTransaccionG2, Collectors.toList())));
        List<EntregaResponse> salida = new ArrayList<>();
        for (Map<String, Object> f : filas) {
            Long id = ((Number) f.get("ID_RECEPCION")).longValue();
            BigDecimal rojo = num(f.get("ROJO_PESO_SIN_FUNDIR_G"));
            BigDecimal verde = num(f.get("VERDE_PESO_SIN_FUNDIR_G"));
            List<Long> idsCompras = porRecepcion.getOrDefault(id, List.of()).stream().sorted().toList();
            salida.add(new EntregaResponse(id, fecha(f.get("FECHA")),
                    (String) f.get("NOMBRE_ACOPIADOR"), rojo, verde, rojo.add(verde), idsCompras));
        }
        return salida;
    }

    /** Oracle DATE puede llegar como Timestamp, sql.Date o LocalDateTime segun el driver. */
    private static LocalDate fecha(Object o) {
        if (o instanceof java.sql.Timestamp t) {
            return t.toLocalDateTime().toLocalDate();
        }
        if (o instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (o instanceof java.time.LocalDateTime l) {
            return l.toLocalDate();
        }
        return LocalDate.parse(o.toString().substring(0, 10));
    }

    private static BigDecimal num(Object o) {
        return o == null ? BigDecimal.ZERO : (o instanceof BigDecimal b ? b : new BigDecimal(o.toString()));
    }

    private static BigDecimal sumar(List<TransaccionG2> lista, String color,
            java.util.function.Function<TransaccionG2, BigDecimal> campo) {
        return lista.stream().filter(t -> color.equals(t.getTipoOro())).map(campo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
