// Espejo de los records del backend (CompraRequest, PartidaRequest, CompraResponse, ...)
export type TipoOro = 'ROJO' | 'VERDE';

export interface PartidaRequest {
  tipoOro: TipoOro;
  pesoSinFundirG: number;
  pesoFundidoNetoG: number;
  precioGramoPen: number;
}

export interface CompraRequest {
  idMinero: number;
  partidas: PartidaRequest[];
  idsPrestamos: number[];
  descuentoManualPen: number;
  motivoDescuento: string | null;
}

export interface PartidaResponse {
  idTransaccionG2: number;
  tipoOro: TipoOro;
  pesoSinFundirG: number;
  pesoFundidoNetoG: number;
  mermaG: number;
  precioAplicadoPen: number;
  totalPagadoPen: number;
  idRecepcionMayorista: number | null;
}

export interface CompraResponse {
  idCompraG2: number;
  idMinero: number;
  nombreMinero: string;
  dniMinero: string;
  fechaCompra: string;
  /** Valor del oro (suma de las partidas). */
  totalPagadoCompraPen: number;
  partidas: PartidaResponse[];
  descuentoPen: number;
  descuentoManualPen: number;
  /** Lo que se entrega al minero: valor del oro - descuentos. */
  totalNetoPen: number;
  idsPrestamos: number[];
  descuentoMotivo: string | null;
}

export interface PrecioOroResponse {
  tipoOro: TipoOro;
  leyAplicadaPct: number;
  precioGramoPen: number;
}

/** Precio publicado por color (GET /api/v1/cotizador/colores). */
export interface CotizacionColor {
  color: TipoOro;
  precioGramoPen: number;
}
