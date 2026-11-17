import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';

import { mensajeError } from '../../core/api/mensaje-error';
import { MineroService } from './minero.service';

@Component({
  selector: 'app-minero-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './minero-form.html',
  styleUrl: './minero-form.css',
})
export class MineroForm implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(MineroService);

  protected readonly id = signal<number | null>(null);
  protected readonly saving = signal(false);
  protected readonly errorServidor = signal('');

  // Estado de la consulta de DNI en RENIEC
  protected readonly consultandoDni = signal(false);
  protected readonly estadoDni = signal('');
  private nombreConsultado = '';
  private ultimoDni = '';

  // Mismas reglas que MineroRequest del backend
  protected readonly form = this.fb.nonNullable.group({
    documentoIdentidad: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(15)]],
    nombresApellidos: ['', [Validators.required, Validators.maxLength(150)]],
    telefono: ['', [Validators.maxLength(20)]],
    zonaProcedencia: ['', [Validators.maxLength(100)]],
  });

  protected get editando(): boolean {
    return this.id() !== null;
  }

  ngOnInit(): void {
    // Flujo reactivo: al escribir 8 digitos se consulta RENIEC (debounce 500 ms) y se rellena el nombre.
    this.form.controls.documentoIdentidad.valueChanges
      .pipe(
        map((dni) => (dni || '').trim()),
        distinctUntilChanged(),
        tap((dni) => {
          if (dni === this.ultimoDni) return;
          this.ultimoDni = dni;
          // Si el nombre vino de un DNI anterior, se limpia al cambiar el DNI
          if (this.nombreConsultado && this.form.controls.nombresApellidos.value === this.nombreConsultado) {
            this.form.controls.nombresApellidos.setValue('');
          }
          this.nombreConsultado = '';
          this.estadoDni.set('');
          this.consultandoDni.set(false);
        }),
        debounceTime(500),
        switchMap((limpio) => {
          if (!/^\d{8}$/.test(limpio)) return of(null);
          this.consultandoDni.set(true);
          this.estadoDni.set('Consultando DNI en RENIEC...');
          return this.service.consultarDni(limpio).pipe(
            map((datos) => (this.form.controls.documentoIdentidad.value.trim() === limpio ? datos : null)),
            catchError(() => {
              if (this.form.controls.documentoIdentidad.value.trim() === limpio) {
                this.estadoDni.set('No se pudo consultar el DNI. Puedes escribir los nombres manualmente.');
              }
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((datos) => {
        this.consultandoDni.set(false);
        if (datos) {
          this.nombreConsultado = datos.nombresApellidos;
          this.form.controls.nombresApellidos.setValue(datos.nombresApellidos);
          this.estadoDni.set('Nombre obtenido por consulta de DNI.');
        }
      });

    // Modo edicion: /mineros/:id/editar
    const valor = this.route.snapshot.paramMap.get('id');
    if (!valor) return;
    const id = Number(valor);
    if (!Number.isInteger(id) || id < 1) {
      this.errorServidor.set('El identificador del minero no es valido.');
      return;
    }
    this.id.set(id);
    this.saving.set(true);
    this.service.obtener(id).subscribe({
      next: (m) => {
        // Al cargar un minero existente no se consulta RENIEC ni se borra su nombre
        this.ultimoDni = (m.documentoIdentidad ?? '').trim();
        this.form.patchValue(
          {
            documentoIdentidad: m.documentoIdentidad ?? '',
            nombresApellidos: m.nombresApellidos ?? '',
            telefono: m.telefono ?? '',
            zonaProcedencia: m.zonaProcedencia ?? '',
          },
          { emitEvent: false },
        );
        this.saving.set(false);
      },
      error: (e) => {
        this.errorServidor.set(mensajeError(e, 'No se pudo cargar el minero solicitado.'));
        this.saving.set(false);
      },
    });
  }

  protected guardar(): void {
    if (this.saving()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.errorServidor.set('');
    const v = this.form.getRawValue();
    const request = {
      documentoIdentidad: v.documentoIdentidad.trim(),
      nombresApellidos: v.nombresApellidos.trim(),
      telefono: v.telefono.trim() || null,
      zonaProcedencia: v.zonaProcedencia.trim() || null,
    };
    const operacion = this.editando
      ? this.service.actualizar(this.id()!, request)
      : this.service.crear(request);
    operacion.subscribe({
      next: () => this.router.navigateByUrl('/mineros'),
      error: (e) => {
        // 409 DNI duplicado, 400 validacion: el mensaje viene de GlobalExceptionHandler
        this.errorServidor.set(mensajeError(e, 'No se pudo guardar el minero.'));
        this.saving.set(false);
      },
    });
  }
}
