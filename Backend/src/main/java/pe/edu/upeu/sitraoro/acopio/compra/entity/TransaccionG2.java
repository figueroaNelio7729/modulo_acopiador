package pe.edu.upeu.sitraoro.acopio.compra.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.sitraoro.acopio.minero.entity.Minero;

/** Detalle de la compra: una partida de oro (ROJO 90% o VERDE 80%). */
@Entity
@Table(name = "TRANSACCIONES_G2")
@Getter
@Setter
@NoArgsConstructor
public class TransaccionG2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_TRANSACCION_G2")
    private Long idTransaccionG2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_COMPRA_G2")
    private CompraG2 compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_MINERO", nullable = false)
    private Minero minero;

    @Column(name = "ID_CENTRO_ACOPIO", nullable = false)
    private Long idCentroAcopio = 1L;

    @Column(name = "TIPO_ORO", nullable = false, length = 10)
    private String tipoOro;

    @Column(name = "PESO_SIN_FUNDIR_G", nullable = false, precision = 10, scale = 3)
    private BigDecimal pesoSinFundirG;

    @Column(name = "PESO_FUNDIDO_NETO_G", nullable = false, precision = 10, scale = 3)
    private BigDecimal pesoFundidoNetoG;

    @Column(name = "PRECIO_APLICADO_PEN", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioAplicadoPen;

    @Column(name = "TOTAL_PAGADO_PEN", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPagadoPen;

    @Column(name = "FECHA_TRANSACCION", nullable = false, updatable = false)
    private LocalDateTime fechaTransaccion = LocalDateTime.now();

    @Column(name = "ANULADA", nullable = false)
    private Integer anulada = 0;

    /** Entrega al mayorista en la que se envio esta partida (null = aun disponible). */
    @Column(name = "ID_RECEPCION_MAYORISTA")
    private Long idRecepcionMayorista;

    /** Liquidacion del mayorista que cerro esta partida (el acopiador no la modifica). */
    @Column(name = "ID_LIQUIDACION_G1")
    private Long idLiquidacionG1;
}
