package pe.edu.upeu.sitraoro.acopio.caja.entity;

/**
 * Origen de un movimiento de caja. PAGO_COMPRA y PRESTAMO los genera el sistema; el resto los registra
 * el acopiador y solo esos se anulan a mano.
 */
public enum OrigenCajaAcopio {
    SALDO_INICIAL(true),
    MAYORISTA(true),
    MANUAL(true),
    PAGO_COMPRA(false),
    PRESTAMO(false);

    private final boolean manual;

    OrigenCajaAcopio(boolean manual) {
        this.manual = manual;
    }

    public boolean esManual() {
        return manual;
    }
}
