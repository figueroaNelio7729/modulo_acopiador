import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormArray, FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of, startWith, switchMap } from 'rxjs';

import { mensajeError } from '../../core/api/mensaje-error';
import { CajaService } from '../caja/caja.service';
import { Prestamo } from '../caja/caja.model';
import { Disponible, Entrega, StockPendiente } from '../entrega/entrega.model';
import { EntregaService } from '../entrega/entrega.service';
import { MineroResponse } from '../minero/minero.model';
import { MineroService } from '../minero/minero.service';
import { ComprobanteCompra } from './comprobante-compra';
import { CompraResponse, TipoOro } from './compra.model';
import { CompraService } from './compra.service';

interface PartidaForm {
  tipoOro: TipoOro;
  pesoSinFundirG: number;
  pesoFundidoNetoG: number;
  precioGramoPen: number;
}

type Pestana = 'entrega' | 'historial' | 'reporte' | 'precio';

@Component({
  selector: 'app-compra-oro',
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe, RouterLink, ComprobanteCompra],
  templateUrl: './compra-oro.html',
  styleUrl: './compra-oro.css',
})
export class CompraOro implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly compraService = inject(CompraService);
  private readonly mineroService = inject(MineroService);
  private readonly cajaService = inject(CajaService);
  private readonly entregaService = inject(EntregaService);

  // Dialogos (modales) de confirmacion
  private readonly dialogoCompra = viewChild.required<ElementRef<HTMLDialogElement>>('confirmacionCompra');
  private readonly dialogoEntrega = viewChild.required<ElementRef<HTMLDialogElement>>('confirmacionEntrega');

  // ---------- Estado (signals) ----------
  protected readonly tab = signal<Pestana>('entrega');
  protected readonly registrando = signal(false);
  protected readonly mineros = signal<MineroResponse[]>([]);
  protected readonly historial = signal<CompraResponse[]>([]);
  protected readonly disponibles = signal<Disponible[]>([]);
  protected readonly stock = signal<StockPendiente | null>(null);
  protected readonly entregas = signal<Entrega[]>([]);
  protected readonly precios = signal<Record<TipoOro, number> | null>(null);
  protected readonly prestamosMinero = signal<Prestamo[]>([]);
  protected readonly prestamosElegidos = signal<number[]>([]);
  protected readonly cargandoPrestamos = signal(false);
  protected readonly seleccion = signal<number[]>([]);
  protected readonly calculando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal('');
  protected readonly mensajeOk = signal('');
  protected readonly comprobante = signal<CompraResponse | null>(null);

  // ---------- Precio oficial (calculadora) ----------
  protected readonly precioForm = this.fb.nonNullable.group({
    onzaUsd: [2650, [Validators.required, Validators.min(1)]],
    tipoCambioPen: [3.75, [Validators.required, Validators.min(0.01)]],
    margenExportacionPct: [5, [Validators.required, Validators.min(0), Validators.max(99)]],
  });

  // ---------- Nueva compra: cabecera + FormArray de partidas ----------
  protected readonly form = this.fb.nonNullable.group({
    idMinero: [0, [Validators.required, Validators.min(1)]],
    partidas: this.fb.array([this.crearPartida()]),
  });
  protected readonly descuentoManual = new FormControl(0, { nonNullable: true, validators: [Validators.min(0)] });
  protected readonly motivoDescuento = new FormControl('', { nonNullable: true, validators: [Validators.maxLength(200)] });

  protected get partidas(): FormArray {
    return this.form.controls.partidas;
  }

  // Los FormControl no son signals: se convierten para poder usar computed().
  private readonly idMineroSig = toSignal(this.form.controls.idMinero.valueChanges.pipe(startWith(0)), { initialValue: 0 });
  private readonly partidasValue = toSignal(
    this.form.controls.partidas.valueChanges.pipe(startWith(this.form.controls.partidas.getRawValue())),
    { initialValue: this.form.controls.partidas.getRawValue() },
  );
  private readonly manualSig = toSignal(this.descuentoManual.valueChanges.pipe(startWith(0)), { initialValue: 0 });
  private readonly motivoSig = toSignal(this.motivoDescuento.valueChanges.pipe(startWith('')), { initialValue: '' });

  /** Merma, % de merma, importe y validez de cada partida. */
  protected readonly detalle = computed(() =>
    (this.partidasValue() as PartidaForm[]).map((p) => {
      const bruto = Number(p.pesoSinFundirG) || 0;
      const fundido = Number(p.pesoFundidoNetoG) || 0;
      const precio = Number(p.precioGramoPen) || 0;
      return {
        recibido: bruto,
        peso: fundido,
        precio,
        merma: bruto - fundido,
        mermaPct: bruto > 0 ? ((bruto - fundido) / bruto) * 100 : 0,
        importe: Math.round(fundido * precio * 100) / 100,
        excede: fundido > bruto,
      };
    }),
  );

  /** Totales de las partidas. */
  protected readonly resumen = computed(() => {
    const d = this.detalle();
    return {
      bruto: d.reduce((s, x) => s + x.recibido, 0),
      fundido: d.reduce((s, x) => s + x.peso, 0),
      merma: d.reduce((s, x) => s + x.merma, 0),
      total: d.reduce((s, x) => s + x.importe, 0),
      hayError: d.some((x) => x.excede),
    };
  });

  protected readonly prestamosADescontar = computed(() =>
    this.prestamosMinero().filter((p) => this.prestamosElegidos().includes(p.idPrestamo)),
  );

  /** Liquidacion del pago: valor del oro - adelantos - otro descuento = neto al minero. */
  protected readonly liquidacion = computed(() => {
    const material = Math.round(this.resumen().total * 100) / 100;
    const prestamos = this.prestamosADescontar().reduce((s, p) => s + p.montoPen, 0);
    const manual = Math.max(Number(this.manualSig()) || 0, 0);
    const descuento = prestamos + manual;
    return {
      material,
      prestamos,
      manual,
      neto: Math.max(Math.round((material - descuento) * 100) / 100, 0),
      excede: descuento > material + 0.001,
      faltaMotivo: manual > 0 && !this.motivoSig().trim(),
    };
  });

  protected readonly mineroCompra = computed(() => {
    const m = this.mineros().find((x) => x.idMinero === Number(this.idMineroSig()));
    return m ? `${m.nombresApellidos} (DNI ${m.documentoIdentidad})` : '';
  });

  /** Nombre del centro (viene del backend con cada entrega) para el comprobante. */
  protected readonly centroNombre = computed(() => this.entregas()[0]?.nombreAcopiador ?? 'Centro de acopio');

  // ---------- Entrega al mayorista ----------
  protected readonly disponiblesRojo = computed(() => this.disponibles().filter((d) => d.tipoOro === 'ROJO'));
  protected readonly disponiblesVerde = computed(() => this.disponibles().filter((d) => d.tipoOro === 'VERDE'));

  protected readonly pesoSeleccionado = computed(() => {
    const lineas = this.disponibles().filter((d) => this.seleccion().includes(d.idTransaccionG2));
    const rojo = lineas.filter((l) => l.tipoOro === 'ROJO').reduce((s, l) => s + l.pesoFundidoNetoG, 0);
    const verde = lineas.filter((l) => l.tipoOro === 'VERDE').reduce((s, l) => s + l.pesoFundidoNetoG, 0);
    return { rojo, verde, total: rojo + verde, lineas };
  });

  constructor() {
    // Al elegir minero: buscar sus adelantos pendientes y marcarlos para descontar
    this.form.controls.idMinero.valueChanges
      .pipe(
        switchMap((id) => {
          this.prestamosMinero.set([]);
          this.prestamosElegidos.set([]);
          if (!Number(id)) return of(null);
          this.cargandoPrestamos.set(true);
          return this.cajaService.listarPrestamos(Number(id), 'PENDIENTE');
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (lista) => {
          this.cargandoPrestamos.set(false);
          if (lista) {
            this.prestamosMinero.set(lista);
            this.prestamosElegidos.set(lista.map((p) => p.idPrestamo));
          }
        },
        error: () => this.cargandoPrestamos.set(false),
      });
  }

  ngOnInit(): void {
    this.cargarDatos();
    this.cargarPreciosPublicados();
  }

  // ---------- Carga de datos (HTTP) ----------
  protected cargarDatos(): void {
    forkJoin({
      mineros: this.mineroService.listar(),
      compras: this.compraService.listar(),
      disponibles: this.entregaService.disponibles(),
      stock: this.entregaService.stock(),
      entregas: this.entregaService.reporte(),
    }).subscribe({
      next: (r) => {
        this.mineros.set(r.mineros);
        this.historial.set(r.compras);
        this.disponibles.set(r.disponibles);
        this.stock.set(r.stock);
        this.entregas.set(r.entregas);
        // Quita de la seleccion lo que ya no esta disponible
        const vigentes = new Set(r.disponibles.map((d) => d.idTransaccionG2));
        this.seleccion.update((s) => s.filter((id) => vigentes.has(id)));
        this.errorGeneral.set('');
      },
      error: (e) => this.errorGeneral.set(mensajeError(e, 'No se pudieron cargar los datos.')),
    });
  }

  /** Precio por gramo publicado para ROJO y VERDE: es el precio de las partidas por defecto. */
  private cargarPreciosPublicados(): void {
    this.compraService.preciosPublicados().subscribe({
      next: (lista) => {
        const rojo = lista.find((c) => c.color === 'ROJO')?.precioGramoPen;
        const verde = lista.find((c) => c.color === 'VERDE')?.precioGramoPen;
        if (rojo && verde) {
          this.precios.set({ ROJO: rojo, VERDE: verde });
          this.partidas.controls.forEach((c) => this.aplicarPrecioOficial(c));
        } else {
          this.calcularPrecios();
        }
      },
      error: () => this.calcularPrecios(),
    });
  }

  // ---------- Calculadora de precio: llamada HTTP al backend ----------
  protected calcularPrecios(): void {
    if (this.precioForm.invalid) {
      this.precioForm.markAllAsTouched();
      return;
    }
    const { onzaUsd, tipoCambioPen, margenExportacionPct } = this.precioForm.getRawValue();
    this.calculando.set(true);
    forkJoin({
      rojo: this.compraService.precioOro('ROJO', onzaUsd, tipoCambioPen, margenExportacionPct),
      verde: this.compraService.precioOro('VERDE', onzaUsd, tipoCambioPen, margenExportacionPct),
    }).subscribe({
      next: ({ rojo, verde }) => {
        this.precios.set({ ROJO: rojo.precioGramoPen, VERDE: verde.precioGramoPen });
        this.partidas.controls.forEach((c) => this.aplicarPrecioOficial(c));
        this.calculando.set(false);
      },
      error: (e) => {
        this.errorGeneral.set(mensajeError(e, 'No se pudo calcular el precio del oro.'));
        this.calculando.set(false);
      },
    });
  }

  // ---------- FormArray de partidas ----------
  private crearPartida() {
    const grupo = this.fb.nonNullable.group({
      tipoOro: ['ROJO' as TipoOro, Validators.required],
      pesoSinFundirG: [0, [Validators.required, Validators.min(0.001)]],
      pesoFundidoNetoG: [0, [Validators.required, Validators.min(0.001)]],
      precioGramoPen: [0, [Validators.required, Validators.min(0.01)]],
    });
    // Al cambiar el color se autocompleta el precio oficial (sigue siendo editable)
    grupo.controls.tipoOro.valueChanges.subscribe(() => this.aplicarPrecioOficial(grupo));
    return grupo;
  }

  private aplicarPrecioOficial(control: AbstractControl): void {
    const p = this.precios();
    if (p) {
      const tipo = control.get('tipoOro')?.value as TipoOro;
      control.get('precioGramoPen')?.setValue(p[tipo]);
    }
  }

  protected agregarPartida(): void {
    const nueva = this.crearPartida();
    this.aplicarPrecioOficial(nueva);
    this.partidas.push(nueva);
  }

  protected quitarPartida(i: number): void {
    if (this.partidas.length > 1) this.partidas.removeAt(i);
  }

  // ---------- Pestanas y apertura del formulario ----------
  protected irA(t: Pestana): void {
    this.registrando.set(false);
    this.tab.set(t);
  }

  protected abrirNuevaCompra(): void {
    this.reiniciarFormulario();
    this.mensajeOk.set('');
    this.errorGeneral.set('');
    this.registrando.set(true);
  }

  private reiniciarFormulario(): void {
    this.partidas.clear();
    this.agregarPartida();
    this.form.controls.idMinero.setValue(0);
    this.descuentoManual.setValue(0);
    this.motivoDescuento.setValue('');
    this.form.markAsUntouched();
  }

  protected alternarPrestamo(id: number, marcado: boolean): void {
    this.prestamosElegidos.update((ids) => (marcado ? [...ids, id] : ids.filter((x) => x !== id)));
  }

  // ---------- Revisar -> Confirmar compra (POST /api/v1/acopio/compras) ----------
  protected revisarCompra(): void {
    this.errorGeneral.set('');
    this.form.markAllAsTouched();
    if (this.form.invalid || this.resumen().hayError) {
      this.errorGeneral.set('Elige el minero y completa los pesos y el precio de cada partida.');
      return;
    }
    const liq = this.liquidacion();
    if (liq.excede) {
      this.errorGeneral.set('Los descuentos superan el valor del oro. Desmarca un adelanto o reduce el descuento.');
      return;
    }
    if (liq.faltaMotivo) {
      this.errorGeneral.set('Escribe el motivo del descuento (por ejemplo: deuda de víveres, herramientas).');
      return;
    }
    this.dialogoCompra().nativeElement.showModal();
  }

  protected cancelarRegistroCompra(): void {
    this.dialogoCompra().nativeElement.close();
  }

  protected confirmarRegistroCompra(): void {
    const v = this.form.getRawValue();
    const manual = this.liquidacion().manual;
    this.guardando.set(true);
    this.compraService
      .registrar({
        idMinero: Number(v.idMinero),
        partidas: v.partidas.map((p) => ({
          tipoOro: p.tipoOro,
          pesoSinFundirG: Number(p.pesoSinFundirG),
          pesoFundidoNetoG: Number(p.pesoFundidoNetoG),
          precioGramoPen: Number(p.precioGramoPen),
        })),
        idsPrestamos: this.prestamosElegidos(),
        descuentoManualPen: manual,
        motivoDescuento: manual > 0 ? this.motivoDescuento.value.trim() : null,
      })
      .subscribe({
        next: (c) => {
          this.guardando.set(false);
          this.dialogoCompra().nativeElement.close();
          this.registrando.set(false);
          this.mensajeOk.set(`Compra #${c.idCompraG2} registrada. Total pagado al minero: ${this.dinero(c.totalNetoPen)}.`);
          this.cargarDatos();
          this.comprobante.set(c); // abre el comprobante digital
        },
        error: (e) => {
          this.guardando.set(false);
          this.dialogoCompra().nativeElement.close();
          this.errorGeneral.set(mensajeError(e, 'No se pudo registrar la compra.'));
        },
      });
  }

  protected verComprobante(c: CompraResponse): void {
    this.comprobante.set(c);
  }

  // ---------- Preparar entrega al mayorista ----------
  protected estaSeleccionada(id: number): boolean {
    return this.seleccion().includes(id);
  }

  protected alternarSeleccion(id: number, marcado: boolean): void {
    this.seleccion.update((s) => (marcado ? [...s, id] : s.filter((x) => x !== id)));
  }

  protected seleccionarColor(lista: Disponible[], marcado: boolean): void {
    const ids = lista.map((d) => d.idTransaccionG2);
    this.seleccion.update((s) => (marcado ? [...new Set([...s, ...ids])] : s.filter((x) => !ids.includes(x))));
  }

  protected todasMarcadas(lista: Disponible[]): boolean {
    return lista.length > 0 && lista.every((d) => this.estaSeleccionada(d.idTransaccionG2));
  }

  protected abrirConfirmacionEntrega(): void {
    if (!this.seleccion().length) return;
    this.dialogoEntrega().nativeElement.showModal();
  }

  protected cancelarEnvioMayorista(): void {
    this.dialogoEntrega().nativeElement.close();
  }

  protected confirmarEnvioMayorista(): void {
    this.enviando.set(true);
    this.entregaService.enviar(this.seleccion()).subscribe({
      next: (e) => {
        this.enviando.set(false);
        this.dialogoEntrega().nativeElement.close();
        this.seleccion.set([]);
        this.errorGeneral.set('');
        this.mensajeOk.set(
          `Entrega enviada al mayorista. Rojo: ${e.rojoG.toFixed(3)} g · Verde: ${e.verdeG.toFixed(3)} g.`,
        );
        this.cargarDatos();
      },
      error: (err) => {
        this.enviando.set(false);
        this.dialogoEntrega().nativeElement.close();
        this.errorGeneral.set(mensajeError(err, 'No se pudo enviar la entrega al mayorista.'));
        this.cargarDatos();
      },
    });
  }

  // ---------- Formato ----------
  protected dinero(valor: number | null | undefined): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor ?? 0);
  }

  protected gramos(valor: number | null | undefined): string {
    return `${new Intl.NumberFormat('es-PE', { minimumFractionDigits: 3, maximumFractionDigits: 3 }).format(valor ?? 0)} g`;
  }
}
