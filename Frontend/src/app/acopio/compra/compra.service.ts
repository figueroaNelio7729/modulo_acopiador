import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CompraRequest,
  CompraResponse,
  CotizacionColor,
  PrecioOroResponse,
  TipoOro,
} from './compra.model';

@Injectable({ providedIn: 'root' })
export class CompraService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/v1/acopio/compras`;

  /** GET /api/v1/acopio/compras/precio-oro?tipoOro=&onzaUsd=&tipoCambioPen=&margenExportacionPct= */
  precioOro(
    tipoOro: TipoOro,
    onzaUsd: number,
    tipoCambioPen: number,
    margenExportacionPct: number,
  ): Observable<PrecioOroResponse> {
    const params = new HttpParams()
      .set('tipoOro', tipoOro)
      .set('onzaUsd', onzaUsd)
      .set('tipoCambioPen', tipoCambioPen)
      .set('margenExportacionPct', margenExportacionPct);
    return this.http.get<PrecioOroResponse>(`${this.url}/precio-oro`, { params });
  }

  /** Ultimo precio por gramo publicado para ROJO y VERDE. */
  preciosPublicados(): Observable<CotizacionColor[]> {
    return this.http.get<CotizacionColor[]>(`${environment.apiBaseUrl}/api/v1/cotizador/colores`);
  }

  /** POST: cabecera + partidas + descuentos en una sola transaccion. */
  registrar(request: CompraRequest): Observable<CompraResponse> {
    return this.http.post<CompraResponse>(this.url, request);
  }

  /** GET: historial con partidas. */
  listar(): Observable<CompraResponse[]> {
    return this.http.get<CompraResponse[]>(this.url);
  }
}
