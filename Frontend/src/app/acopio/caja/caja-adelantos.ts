import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { mensajeError } from '../../core/api/mensaje-error';
import { MineroResponse } from '../minero/minero.model';
import { MineroService } from '../minero/minero.service';
import { EstadoPrestamo, Movimiento, Prestamo, ResumenCaja } from './caja.model';
import { CajaService } from './caja.service';

@Component({
  selector: 'app-caja-adelantos',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe],
  templateUrl: './caja-adelantos.html',
  styleUrl: './caja-adelantos.css',
})
export class CajaAdelantos implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly cajaService = inject(CajaService);
  private readonly mineroService = inject(MineroService);

  protected readonly resumen = signal<ResumenCaja | null>(null);
  protected readonly movimientos = signal<Movimiento[]>([]);
  protected readonly prestamos = signal<Prestamo[]>([]);
  protected readonly mineros = signal<MineroResponse[]>([]);
  protected readonly filtroEstado = signal<EstadoPrestamo | ''>('PENDIENTE');
  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly ok = signal('');

  // Ingreso / egreso manual o saldo inicial
  protected readonly movForm = this.fb.nonNullable.group({
    tipo: ['INGRESO' as 'INGRESO' | 'EGRESO', Validators.required],
    monto: [0, [Validators.required, Validators.min(0.01)]],
    concepto: ['', [Validators.required, Validators.maxLength(200)]],
    saldoInicial: [false],
  });

  // Adelanto a un minero (sale de la caja)
  protected readonly adelantoForm = this.fb.nonNullable.group({
    idMinero: [0, [Validators.required, Validators.min(1)]],
    montoPen: [0, [Validators.required, Validators.min(0.01)]],
    observacion: ['', [Validators.maxLength(200)]],
  });

  ngOnInit(): void {
    this.mineroService.listar().subscribe((m) => this.mineros.set(m));
    this.cargar();
  }

  protected cargar(): void {
    this.cajaService.resumen().subscribe({
      next: (r) => {
        this.resumen.set(r);
        this.movimientos.set(r.ultimos);
      },
      error: (e) => this.error.set(mensajeError(e, 'No se pudo cargar la caja.')),
    });
    this.cajaService.listarPrestamos(undefined, this.filtroEstado() || undefined).subscribe({
      next: (p) => this.prestamos.set(p),
      error: (e) => this.error.set(mensajeError(e, 'No se pudieron cargar los adelantos.')),
    });
  }

  protected cambiarFiltro(valor: string): void {
    this.filtroEstado.set(valor as EstadoPrestamo | '');
    this.cargar();
  }

  protected registrarMovimiento(): void {
    if (this.movForm.invalid) {
      this.movForm.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    const v = this.movForm.getRawValue();
    this.cajaService
      .registrarMovimiento({ tipo: v.tipo, monto: Number(v.monto), concepto: v.concepto.trim(), saldoInicial: v.saldoInicial })
      .subscribe({
        next: () => {
          this.ok.set('Movimiento registrado en caja.');
          this.movForm.reset({ tipo: 'INGRESO', monto: 0, concepto: '', saldoInicial: false });
          this.guardando.set(false);
          this.cargar();
        },
        error: (e) => {
          this.error.set(mensajeError(e, 'No se pudo registrar el movimiento.'));
          this.guardando.set(false);
        },
      });
  }

  protected anularMovimiento(m: Movimiento): void {
    if (!confirm(`¿Anular el movimiento "${m.concepto}"?`)) return;
    this.cajaService.anularMovimiento(m.idMovimiento).subscribe({
      next: () => this.cargar(),
      error: (e) => this.error.set(mensajeError(e, 'No se pudo anular el movimiento.')),
    });
  }

  protected registrarAdelanto(): void {
    if (this.adelantoForm.invalid) {
      this.adelantoForm.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    const v = this.adelantoForm.getRawValue();
    this.cajaService
      .registrarPrestamo({ idMinero: Number(v.idMinero), montoPen: Number(v.montoPen), observacion: v.observacion.trim() || null })
      .subscribe({
        next: (p) => {
          this.ok.set(`Adelanto N.º ${p.idPrestamo} registrado para ${p.minero}. Se descontará en su próxima compra.`);
          this.adelantoForm.reset({ idMinero: 0, montoPen: 0, observacion: '' });
          this.guardando.set(false);
          this.cargar();
        },
        // 409: no alcanza el saldo de caja
        error: (e) => {
          this.error.set(mensajeError(e, 'No se pudo registrar el adelanto.'));
          this.guardando.set(false);
        },
      });
  }

  protected anularAdelanto(p: Prestamo): void {
    if (!confirm(`¿Anular el adelanto N.º ${p.idPrestamo} de ${p.minero}?`)) return;
    this.cajaService.anularPrestamo(p.idPrestamo).subscribe({
      next: () => this.cargar(),
      error: (e) => this.error.set(mensajeError(e, 'No se pudo anular el adelanto.')),
    });
  }
}
