import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { mensajeError } from '../../core/api/mensaje-error';
import { MineroResponse } from './minero.model';
import { MineroService } from './minero.service';

@Component({
  selector: 'app-minero-list',
  imports: [RouterLink, DatePipe],
  templateUrl: './minero-list.html',
  styleUrl: './minero-list.css',
})
export class MineroList implements OnInit {
  private readonly service = inject(MineroService);

  protected readonly mineros = signal<MineroResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly ok = signal('');

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.loading.set(true);
    this.error.set('');
    this.service.listar().subscribe({
      next: (datos) => {
        this.mineros.set(datos);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(mensajeError(e, 'No se pudo cargar la lista de mineros.'));
        this.loading.set(false);
      },
    });
  }

  protected eliminar(m: MineroResponse): void {
    if (!confirm(`¿Eliminar al minero ${m.nombresApellidos}? Esta acción no se puede deshacer.`)) return;
    this.error.set('');
    this.ok.set('');
    this.service.eliminar(m.idMinero).subscribe({
      next: () => {
        this.ok.set('Minero eliminado.');
        this.cargar();
      },
      // 409 si ya tiene compras o adelantos
      error: (e) => this.error.set(mensajeError(e, 'No se pudo eliminar el minero.')),
    });
  }
}
