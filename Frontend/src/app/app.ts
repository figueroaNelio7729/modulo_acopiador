import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { environment } from '../environments/environment';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /** Estado de la conexion con el backend / Oracle (GET /actuator/health). */
  protected readonly conexion = signal<'verificando' | 'activa' | 'caida'>('verificando');
  protected readonly fechaHora = signal(this.ahora());
  private readonly reloj = setInterval(() => this.fechaHora.set(this.ahora()), 1000);

  constructor() {
    this.http.get<{ status: string }>(`${environment.apiBaseUrl}/actuator/health`).subscribe({
      next: (r) => this.conexion.set(r.status === 'UP' ? 'activa' : 'caida'),
      error: () => this.conexion.set('caida'),
    });
  }

  /** Sin autenticacion en esta version: solo vuelve al inicio del modulo. */
  protected salir(): void {
    this.router.navigateByUrl('/compras');
  }

  private ahora(): string {
    return new Date().toLocaleString('es-PE', { dateStyle: 'short', timeStyle: 'medium' });
  }

  ngOnDestroy(): void {
    clearInterval(this.reloj);
  }
}
