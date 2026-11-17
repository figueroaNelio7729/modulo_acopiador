import { Routes } from '@angular/router';

// Lazy loading: cada pantalla se descarga solo cuando se visita
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'compras' },
  {
    path: 'compras',
    title: 'Compras de oro | SITRA-ORO',
    loadComponent: () => import('./acopio/compra/compra-oro').then((m) => m.CompraOro),
  },
  {
    path: 'caja',
    title: 'Caja y adelantos | SITRA-ORO',
    loadComponent: () => import('./acopio/caja/caja-adelantos').then((m) => m.CajaAdelantos),
  },
  {
    path: 'mineros',
    title: 'Mineros | SITRA-ORO',
    loadComponent: () => import('./acopio/minero/minero-list').then((m) => m.MineroList),
  },
  {
    path: 'mineros/nuevo',
    title: 'Registrar minero | SITRA-ORO',
    loadComponent: () => import('./acopio/minero/minero-form').then((m) => m.MineroForm),
  },
  {
    path: 'mineros/:id/editar',
    title: 'Editar minero | SITRA-ORO',
    loadComponent: () => import('./acopio/minero/minero-form').then((m) => m.MineroForm),
  },
  {
    path: 'mercado',
    title: 'Mercado del oro | SITRA-ORO',
    loadComponent: () => import('./acopio/mercado/mercado-acopio').then((m) => m.MercadoAcopio),
  },
  { path: '**', redirectTo: 'compras' },
];
