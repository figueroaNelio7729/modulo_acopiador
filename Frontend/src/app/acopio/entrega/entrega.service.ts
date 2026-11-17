import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Disponible, Entrega, StockPendiente } from './entrega.model';

@Injectable({ providedIn: 'root' })
export class EntregaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/v1/acopio/entregas-mayorista`;

  disponibles(): Observable<Disponible[]> {
    return this.http.get<Disponible[]>(`${this.url}/disponibles`);
  }

  stock(): Observable<StockPendiente> {
    return this.http.get<StockPendiente>(`${this.url}/stock`);
  }

  /** Envia las partidas seleccionadas al mayorista: dejan de estar disponibles. */
  enviar(idsComprasAcopiador: number[]): Observable<Entrega> {
    return this.http.post<Entrega>(this.url, { idsComprasAcopiador });
  }

  reporte(): Observable<Entrega[]> {
    return this.http.get<Entrega[]>(this.url);
  }
}
