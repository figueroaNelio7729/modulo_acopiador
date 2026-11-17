package pe.edu.upeu.sitraoro.acopio.caja.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entrada o salida de dinero de la caja del centro (solo soles). */
@Entity
@Table(name = "ACOPIO_CAJA_MOVIMIENTOS")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoCajaAcopio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_MOVIMIENTO")
    private Long idMovimiento;

    @Column(name = "ID_CENTRO_ACOPIO", nullable = false)
    private Long idCentroAcopio;

    @Column(name = "FECHA", nullable = false)
    private LocalDate fecha;

    @Column(name = "MONEDA", nullable = false, length = 3)
    private String moneda = "PEN";

    /** true = ingreso, false = egreso. */
    @Column(name = "INGRESO", nullable = false)
    private boolean ingreso;

    @Enumerated(EnumType.STRING)
    @Column(name = "ORIGEN", nullable = false, length = 15)
    private OrigenCajaAcopio origen;

    @Column(name = "MONTO", nullable = false, precision = 16, scale = 2)
    private BigDecimal monto;

    @Column(name = "CONCEPTO", nullable = false, length = 800)
    private String concepto;

    /** Compra (PAGO_COMPRA) o adelanto (PRESTAMO) que origino el movimiento. */
    @Column(name = "ID_REFERENCIA")
    private Long idReferencia;

    @Column(name = "ANULADO", nullable = false)
    private boolean anulado = false;

    @Column(name = "FECHA_CREACION", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
