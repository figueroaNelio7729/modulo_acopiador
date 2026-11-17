package pe.edu.upeu.sitraoro.acopio.prestamo.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.sitraoro.acopio.minero.entity.Minero;

/** Dinero que el acopiador adelanta a un minero (soles); se descuenta al pagarle su oro. */
@Entity
@Table(name = "ACOPIO_PRESTAMOS_MINERO")
@Getter
@Setter
@NoArgsConstructor
public class PrestamoMinero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PRESTAMO")
    private Long idPrestamo;

    @Column(name = "ID_CENTRO_ACOPIO", nullable = false)
    private Long idCentroAcopio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_MINERO", nullable = false)
    private Minero minero;

    @Column(name = "FECHA", nullable = false)
    private LocalDate fecha;

    @Column(name = "MONTO_PEN", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoPen;

    @Column(name = "OBSERVACION", length = 800)
    private String observacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 12)
    private EstadoPrestamo estado = EstadoPrestamo.PENDIENTE;

    /** Compra en la que se descontó. */
    @Column(name = "ID_COMPRA_G2")
    private Long idCompraG2;

    @Column(name = "FECHA_CREACION", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
