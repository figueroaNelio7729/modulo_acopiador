import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  EstadoPrestamo,
  Movimiento,
  MovimientoManualRequest,
  Prestamo,
  PrestamoRequest,
  ResumenCaja,
} from './caja.model';

@Injectable({ providedIn: 'root' })
export class CajaService {
  private readonly http = inject(HttpClient);
  private readonly caja = `${environment.apiBaseUrl}/api/v1/acopio/caja`;
  private readonly prestamos = `${environment.apiBaseUrl}/api/v1/acopio/prestamos`;

  resumen(): Observable<ResumenCaja> {
    return this.http.get<ResumenCaja>(this.caja);
  }

  registrarMovimiento(request: MovimientoManualRequest): Observable<Movimiento> {
    return this.http.post<Movimiento>(`${this.caja}/movimientos`, request);
  }

  anularMovimiento(id: number): Observable<Movimiento> {
    return this.http.patch<Movimiento>(`${this.caja}/movimientos/${id}/anular`, {});
  }

  listarPrestamos(idMinero?: number, estado?: EstadoPrestamo): Observable<Prestamo[]> {
    let params = new HttpParams();
    if (idMinero) params = params.set('idMinero', idMinero);
    if (estado) params = params.set('estado', estado);
    return this.http.get<Prestamo[]>(this.prestamos, { params });
  }

  registrarPrestamo(request: PrestamoRequest): Observable<Prestamo> {
    return this.http.post<Prestamo>(this.prestamos, request);
  }

  anularPrestamo(id: number): Observable<Prestamo> {
    return this.http.patch<Prestamo>(`${this.prestamos}/${id}/anular`, {});
  }
}
