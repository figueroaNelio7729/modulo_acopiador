import { HttpErrorResponse } from '@angular/common/http';

/** Mensaje legible de un error HTTP: usa el "mensaje" que devuelve GlobalExceptionHandler del backend. */
export function mensajeError(e: unknown, porDefecto: string): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) return 'No se pudo conectar con el backend (puerto 8082).';
    const m = e.error?.mensaje;
    if (typeof m === 'string' && m) return m;
  }
  return porDefecto;
}
