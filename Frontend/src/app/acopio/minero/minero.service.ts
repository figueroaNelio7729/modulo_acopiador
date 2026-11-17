import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { MineroRequest, MineroResponse } from './minero.model';

@Injectable({ providedIn: 'root' })
export class MineroService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/v1/mineros`;

  listar(): Observable<MineroResponse[]> {
    return this.http.get<MineroResponse[]>(this.url);
  }

  obtener(id: number): Observable<MineroResponse> {
    return this.http.get<MineroResponse>(`${this.url}/${id}`);
  }

  crear(request: MineroRequest): Observable<MineroResponse> {
    return this.http.post<MineroResponse>(this.url, request);
  }

  actualizar(id: number, request: MineroRequest): Observable<MineroResponse> {
    return this.http.put<MineroResponse>(`${this.url}/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  /** Nombre completo desde RENIEC (el backend llama a la API externa). */
  consultarDni(dni: string): Observable<{ nombresApellidos: string }> {
    return this.http.get<{ nombresApellidos: string }>(
      `${environment.apiBaseUrl}/api/v1/publico/identidad/dni/${dni}`,
    );
  }
}
