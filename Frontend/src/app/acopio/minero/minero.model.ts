// Espejo de los records del backend: MineroRequest / MineroResponse
export interface MineroRequest {
  documentoIdentidad: string;
  nombresApellidos: string;
  telefono: string | null;
  zonaProcedencia: string | null;
}

export interface MineroResponse {
  idMinero: number;
  documentoIdentidad: string;
  nombresApellidos: string;
  telefono: string | null;
  zonaProcedencia: string | null;
  fechaRegistro: string;
}
