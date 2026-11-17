export interface Movimiento {
  idMovimiento: number;
  fecha: string;
  tipo: 'INGRESO' | 'EGRESO';
  origen: 'SALDO_INICIAL' | 'MAYORISTA' | 'MANUAL' | 'PAGO_COMPRA' | 'PRESTAMO';
  monto: number;
  concepto: string;
  idReferencia: number | null;
  anulado: boolean;
  anulable: boolean;
}

export interface ResumenCaja {
  saldoPen: number;
  ingresosMes: number;
  egresosMes: number;
  ultimos: Movimiento[];
}

export interface MovimientoManualRequest {
  tipo: 'INGRESO' | 'EGRESO';
  monto: number;
  concepto: string;
  fecha?: string | null;
  saldoInicial: boolean;
}

export type EstadoPrestamo = 'PENDIENTE' | 'DESCONTADO' | 'ANULADO';

export interface Prestamo {
  idPrestamo: number;
  idMinero: number;
  minero: string;
  documentoMinero: string;
  fecha: string;
  montoPen: number;
  observacion: string | null;
  estado: EstadoPrestamo;
  idCompraG2: number | null;
}

export interface PrestamoRequest {
  idMinero: number;
  montoPen: number;
  observacion: string | null;
}
