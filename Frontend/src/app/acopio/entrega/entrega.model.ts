import { TipoOro } from '../compra/compra.model';

export interface Disponible {
  idTransaccionG2: number;
  idCompraG2: number | null;
  tipoOro: TipoOro;
  idMinero: number;
  minero: string;
  documentoMinero: string;
  pesoSinFundirG: number;
  pesoFundidoNetoG: number;
  precioAplicadoPen: number;
  totalPagadoPen: number;
  fecha: string;
}

export interface StockPendiente {
  rojoG: number;
  rojoPen: number;
  verdeG: number;
  verdePen: number;
}

export interface Entrega {
  idRecepcion: number;
  fecha: string;
  nombreAcopiador: string;
  rojoG: number;
  verdeG: number;
  totalG: number;
  idsComprasAcopiador: number[];
}
