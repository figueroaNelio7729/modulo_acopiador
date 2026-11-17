package pe.edu.upeu.sitraoro.acopio.compra.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.sitraoro.acopio.minero.entity.Minero;

/** Cabecera de la compra: un minero entrega una o varias partidas de oro. */
@Entity
@Table(name = "COMPRAS_G2")
@Getter
@Setter
@NoArgsConstructor
public class CompraG2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_COMPRA_G2")
    private Long idCompraG2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_MINERO", nullable = false)
    private Minero minero;

    @Column(name = "ID_CENTRO_ACOPIO", nullable = false)
    private Long idCentroAcopio = 1L;

    @Column(name = "FECHA_COMPRA", nullable = false, updatable = false)
    private LocalDateTime fechaCompra = LocalDateTime.now();

    /** Descuento total = adelantos descontados + descuento manual. */
    @Column(name = "DESCUENTO_PEN", nullable = false, precision = 14, scale = 2)
    private BigDecimal descuentoPen = BigDecimal.ZERO;

    @Column(name = "DESCUENTO_MANUAL_PEN", nullable = false, precision = 14, scale = 2)
    private BigDecimal descuentoManualPen = BigDecimal.ZERO;

    @Column(name = "DESCUENTO_MOTIVO", length = 200)
    private String descuentoMotivo;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.PERSIST)
    private List<TransaccionG2> detalles = new ArrayList<>();

    public void agregarPartida(TransaccionG2 partida) {
        partida.setCompra(this);
        this.detalles.add(partida);
    }
}
