# Guía de defensa · Módulo Acopiador (SITRA-ORO)

> Todo lo que aparece aquí está tomado del código real de `C:\laragon\modulo_acopiador` (carpetas `Backend/` y `Frontend/`).
> Las rutas son relativas a esa carpeta. Los números de línea corresponden a la versión actual.

---

## PARTE 1 · Mapa del sistema

### 1.1 Arquitectura en una frase
**Monolito modular** en Spring Boot 4.0.7 (Java 21) conectado a Oracle 23c, y un frontend **Angular standalone** que lo consume por HTTP/JSON. Igual que el modelo del docente: primero el **módulo de negocio** (`acopio`) y dentro cada **submódulo** con sus 5 capas: `controller → service → repository → entity` y `dto`.

```
Navegador (Angular, puerto 4201)
   │  HttpClient → JSON
   ▼
Controller  (@RestController)        recibe la petición y valida con @Valid
   ▼
Service     (@Service, @Transactional) aplica las reglas de negocio
   ▼
Repository  (JpaRepository / JdbcTemplate) consulta y guarda
   ▼
Entity      (@Entity)  ⇄  Oracle FREEPDB1 (usuario BOMERP_APP, puerto 1522)
```

### 1.2 Carpetas del backend (`Backend/src/main/java/pe/edu/upeu/sitraoro/`)

| Paquete | Qué contiene |
|---|---|
| raíz | `SitraOroApplication`, `OpenApiConfig`, `CorsConfig`, `CentroActual`, `GlobalExceptionHandler`, `RecursoNoEncontradoException`, `ConflictoNegocioException` |
| `acopio/minero` | Registro de mineros + consulta de DNI en RENIEC |
| `acopio/compra` | Compra de oro (cabecera + partidas), cálculo de precio y merma |
| `acopio/prestamo` | Adelantos (préstamos) a mineros |
| `acopio/caja` | Caja del centro en soles |
| `acopio/entrega` | Envío de compras al mayorista |
| `acopio/cotizador` | Precio por gramo publicado (solo lectura) |

### 1.3 Carpetas del frontend (`Frontend/src/app/`)

| Archivo / carpeta | Qué contiene |
|---|---|
| `app.ts`, `app.html` | Layout: cabecera (reloj, usuario, "Cerrar sesión") y menú lateral |
| `app.routes.ts` | Rutas con *lazy loading* |
| `app.config.ts` | `provideRouter` y `provideHttpClient` |
| `acopio/minero/` | `minero-list`, `minero-form`, `minero.service`, `minero.model` |
| `acopio/caja/` | `caja-adelantos`, `caja.service`, `caja.model` |
| `acopio/compra/` | `compra-oro` (pantalla principal), `comprobante-compra`, `compra.service`, `compra.model` |
| `acopio/entrega/` | `entrega.service`, `entrega.model` |
| `acopio/mercado/` | `mercado-acopio`, `market-chart` (copiados de SITRA-ORO) |
| `core/api/` | `mensaje-error.ts`, `mercado-live.service.ts`, `market-cotizaciones.service.ts` |
| `../environments/environment.ts` | `apiBaseUrl: 'http://localhost:8082'` |

---

## PARTE 2 · Backend

### 2.1 Controladores y endpoints

| Controlador | Endpoint | Método del controlador |
|---|---|---|
| `MineroController` | `POST /api/v1/mineros` | `crear` (línea 35) |
| | `GET /api/v1/mineros` | `listar` (41) |
| | `GET /api/v1/mineros/{id}` | `obtener` (47) |
| | `PUT /api/v1/mineros/{id}` | `actualizar` (53) |
| | `DELETE /api/v1/mineros/{id}` | `eliminar` (60) |
| `ConsultaDniController` | `GET /api/v1/publico/identidad/dni/{dni}` | `consultar` (49) |
| `CompraController` | `GET /api/v1/acopio/compras/precio-oro` | `calcularPrecio` (36) |
| | `POST /api/v1/acopio/compras` | `registrar` (51) |
| | `GET /api/v1/acopio/compras` | `listar` (57) |
| `PrestamoMineroController` | `POST /api/v1/acopio/prestamos` | `registrar` (36) |
| | `GET /api/v1/acopio/prestamos?idMinero=&estado=` | `listar` (42) |
| | `PATCH /api/v1/acopio/prestamos/{id}/anular` | `anular` (49) |
| `CajaAcopioController` | `GET /api/v1/acopio/caja` | `resumen` (37) |
| | `GET /api/v1/acopio/caja/movimientos` | `movimientos` (43) |
| | `POST /api/v1/acopio/caja/movimientos` | `registrar` (52) |
| | `PATCH /api/v1/acopio/caja/movimientos/{id}/anular` | `anular` (58) |
| `EntregaMayoristaController` | `GET /api/v1/acopio/entregas-mayorista/disponibles` | `disponibles` (33) |
| | `GET /api/v1/acopio/entregas-mayorista/stock` | `stock` (39) |
| | `POST /api/v1/acopio/entregas-mayorista` | `registrar` (46) |
| | `GET /api/v1/acopio/entregas-mayorista` | `reporte` (52) |
| `CotizadorController` | `GET /api/v1/cotizador/colores` | `colores` (31) |

`CompraController` y `PrestamoMineroController` también responden en `/api/v1/compras` y `/api/v1/prestamos` (alias en `@RequestMapping({ ..., ... })`).

### 2.2 Servicios y reglas de negocio

| Servicio | Regla de negocio | Dónde |
|---|---|---|
| `MineroServiceImpl` | No puede haber dos mineros con el mismo DNI → 409 | `crear` línea 28, `actualizar` línea 54 |
| | No se elimina un minero con compras o adelantos → 409 | `eliminar` líneas 64-73 |
| | Un minero nuevo queda asignado al centro actual | `crear` (`m.setIdCentroAcopio(centro.id())`) |
| `CalculadorPrecioOro` | Precio/g = Onza × TC × Ley × (1 − Export%) ÷ 31.0999 (ROJO 0.90, VERDE 0.80) | `calcularPrecioGramoPen` línea 18 |
| | El peso fundido no puede superar al recibido → 400 | `calcularMermaGramos` líneas 27-33 |
| | Importe = peso fundido × precio/g (2 decimales) | `calcularImportePartida` línea 36 |
| `CompraServiceImpl` | Una compra = 1 cabecera + N partidas, todo en una transacción | `registrarCompra` líneas 36-87 |
| | Descuento manual > 0 exige motivo → 400 | líneas 68-71 |
| | Adelantos + descuento manual no pueden superar el valor del oro → 400 | líneas 75-78 |
| | El neto pagado sale de la caja | línea 84 |
| `PrestamoMineroService` | El adelanto sale de la caja el día que se entrega | `registrar` línea 57 |
| | Solo se anula un adelanto PENDIENTE → 409 | `anular` línea 72 |
| | Al descontar: el adelanto debe ser del mismo minero y estar PENDIENTE, y pasa a DESCONTADO | `descontar` líneas 90-115 |
| `CajaAcopioService` | Saldo = ingresos − egresos no anulados | consulta `saldo` del repositorio |
| | No se entrega un adelanto mayor que el saldo → 409 | `registrarPrestamo` líneas 97-106 |
| | Solo se anulan a mano los movimientos manuales | `anular` líneas 70-81 |
| `EntregaMayoristaService` | Solo se envían partidas del centro, no anuladas, no enviadas y no liquidadas → 409 | `registrar` líneas 74-87 |
| | Crea la recepción en `MAYORISTA_RECEPCIONES` y marca cada partida con `ID_RECEPCION_MAYORISTA` | líneas 99-117 |

### 2.3 Repositorios y consultas

| Repositorio | Consulta | Para qué |
|---|---|---|
| `MineroRepository` | `existsByDocumentoIdentidad` (línea 8) | Detectar DNI duplicado al crear |
| | `existsByDocumentoIdentidadAndIdMineroNot` (línea 10) | DNI duplicado al editar (excluye al mismo minero) |
| `CompraG2Repository` | `findByIdCentroAcopioOrderByFechaCompraDescIdCompraG2Desc` + `@EntityGraph({"minero","detalles"})` (líneas 13-14) | Historial con minero y partidas en una sola consulta (evita N+1) |
| `TransaccionG2Repository` | `disponibles` JPQL (líneas 17-23) | Partidas `anulada = 0`, sin `idRecepcionMayorista` ni `idLiquidacionG1` |
| | `bloquearPorIds` con `@Lock(PESSIMISTIC_WRITE)` (líneas 25-27) | Bloquear las filas al enviarlas (evita doble envío simultáneo) |
| | `findByIdRecepcionMayoristaIn` (línea 29) | Partidas de cada entrega (reporte) |
| `PrestamoMineroRepository` | `buscar` con filtros opcionales `(:x IS NULL OR ...)` (líneas 18-27) | Adelantos por minero y estado |
| | `bloquear` con `@Lock(PESSIMISTIC_WRITE)` (líneas 29-32) | Bloquear adelantos al descontarlos |
| | `findByIdCompraG2` (línea 34) | Qué adelantos se descontaron en una compra |
| `MovimientoCajaAcopioRepository` | `buscar` (líneas 17-25) | Movimientos por rango de fechas |
| | `saldo` con `SUM(CASE WHEN ingreso ...)` (líneas 27-32) | Saldo de caja |
| | `findByOrigenAndIdReferenciaAndAnuladoFalse` (línea 34) | Anular el egreso automático de un adelanto anulado |
| `JdbcTemplate` (sin repositorio) | `INSERT INTO MAYORISTA_RECEPCIONES` (`EntregaMayoristaService` líneas 99-114) y `SELECT ... COTIZACIONES_COLOR` (`CotizadorController`) | Tablas del mayorista que no tienen entidad en este proyecto |

### 2.4 Entidades y relaciones

| Entidad | Tabla Oracle | Relaciones |
|---|---|---|
| `Minero` | `MINEROS` | (ninguna hacia otras entidades) |
| `CompraG2` | `COMPRAS_G2` | `@ManyToOne Minero` (línea 37) · `@OneToMany(mappedBy="compra", cascade=PERSIST) List<TransaccionG2> detalles` (línea 57) |
| `TransaccionG2` | `TRANSACCIONES_G2` | `@ManyToOne CompraG2 compra` (línea 33, columna `ID_COMPRA_G2`) · `@ManyToOne Minero` (línea 37) |
| `PrestamoMinero` | `ACOPIO_PRESTAMOS_MINERO` | `@ManyToOne(optional=false) Minero` (línea 40) · `idCompraG2` como número (no relación) |
| `MovimientoCajaAcopio` | `ACOPIO_CAJA_MOVIMIENTOS` | `idReferencia` = id de la compra o del adelanto que lo originó |

```
MINEROS 1 ──< COMPRAS_G2 1 ──< TRANSACCIONES_G2 >── 1 MINEROS
   │                                   │
   └──< ACOPIO_PRESTAMOS_MINERO         └── ID_RECEPCION_MAYORISTA → MAYORISTA_RECEPCIONES
```

`CompraG2` (cabecera) y `TransaccionG2` (detalle) son el patrón **cabecera-detalle**. El método `agregarPartida` (`CompraG2.java` línea 60) mantiene los dos lados sincronizados:

```java
public void agregarPartida(TransaccionG2 partida) {
    partida.setCompra(this);
    this.detalles.add(partida);
}
```

### 2.5 Manejo de errores (`GlobalExceptionHandler.java`)

| Excepción | HTTP | Línea |
|---|---|---|
| `MethodArgumentNotValidException` (falló `@Valid`) | 400 + campos | 19 |
| `HttpMessageNotReadableException` (JSON roto) | 400 | 29 |
| `IllegalArgumentException` | 400 | 34 |
| `RecursoNoEncontradoException` | 404 | 39 |
| `ConflictoNegocioException` | 409 | 44 |
| `ResponseStatusException` (consulta DNI) | el que indique | 49 |
| `Exception` (cualquier otra) | 500 sin detalles técnicos | 55 |

Todas devuelven el mismo formato JSON: `timestamp`, `status`, `error`, `mensaje`. El frontend muestra `mensaje` con `mensajeError()` (`Frontend/src/app/core/api/mensaje-error.ts`).

### 2.6 Configuración

- `Backend/src/main/resources/application.yml`: activa el perfil `dev`.
- `Backend/src/main/resources/application-dev.yml`: puerto **8082**, conexión `jdbc:oracle:thin:@localhost:1522/FREEPDB1` (línea 6), `ddl-auto: validate` (línea 13): Hibernate **comprueba** que las tablas coinciden con las entidades, pero **no las crea**. También tiene `sitra.acopio.id-centro: 1`, el token de RENIEC y el CORS de Actuator.
- `CorsConfig.java` línea 15: permite `http://localhost:4200` y `http://localhost:4201` en `/api/**`.
- `CentroActual.java`: como no hay login, el centro de acopio sale de la configuración (`@Value("${sitra.acopio.id-centro:1}")`).
- `Backend/src/test/java/.../ModularityTests.java`: `ApplicationModules.of(SitraOroApplication.class).verify()` comprueba con Spring Modulith que no hay dependencias prohibidas entre módulos.

---

## PARTE 3 · Frontend

### 3.1 Comunicación con el backend
1. `environment.ts` define `apiBaseUrl: 'http://localhost:8082'`.
2. `app.config.ts` línea 11 registra `provideHttpClient()` (sin interceptores: no hay JWT).
3. Cada **servicio** arma la URL y llama con `HttpClient`:
   - `minero.service.ts`: `get`, `post`, `put`, `delete` sobre `/api/v1/mineros` (líneas 14-30) y `consultarDni` (35).
   - `compra.service.ts`: `precioOro` (31), `preciosPublicados` (36), `registrar` (41), `listar` (46).
   - `caja.service.ts`: caja (22-30) y adelantos (37-45).
   - `entrega.service.ts`: `disponibles` (14), `stock` (18), `enviar` (23), `reporte` (27).
4. Los métodos devuelven `Observable`. El componente se **suscribe** (`.subscribe({ next, error })`) y guarda el resultado en un **signal**.
5. El navegador solo permite estas peticiones porque el backend devuelve las cabeceras CORS (`CorsConfig`).

### 3.2 Componentes visuales y sus botones

**`app.html` (layout)**
| Elemento | Línea | Qué hace |
|---|---|---|
| Menú "Compras de oro / Caja y adelantos / Mineros / Mercado del oro" | 17-20 | `routerLink` cambia de ruta sin recargar |
| "Cerrar sesión" | 11 | `salir()` (`app.ts` 30): solo vuelve a `/compras`; **no hay sesión real** |
| Indicador "Oracle conectado" | — | `GET /actuator/health` en el constructor de `app.ts` (línea 23) |

**`minero-list.html`**
| Botón | Línea | Evento → qué pasa |
|---|---|---|
| Actualizar | 9 | `cargar()` → `GET /api/v1/mineros` |
| Registrar minero | 10 | `routerLink="/mineros/nuevo"` → abre `MineroForm` |
| Editar | 47 | `routerLink` a `/mineros/:id/editar` |
| Eliminar | 48 | `eliminar(m)` → `confirm()` → `DELETE /api/v1/mineros/{id}` (409 si tiene movimientos) |

**`minero-form.html`**
| Elemento | Línea | Evento → qué pasa |
|---|---|---|
| Campo DNI | — | `valueChanges` + `debounceTime(500)` + `switchMap` (`minero-form.ts` 47-90) → con 8 dígitos llama a `GET /api/v1/publico/identidad/dni/{dni}` y rellena el nombre |
| Guardar | 48 (`type="submit"`) | `(ngSubmit)="guardar()"` (línea 10) → `POST` (nuevo) o `PUT` (editar) |
| Cancelar | 51 | vuelve a `/mineros` |

**`caja-adelantos.html`**
| Botón | Línea | Evento → qué pasa |
|---|---|---|
| Registrar en caja | 43 | `registrarMovimiento()` → `POST /api/v1/acopio/caja/movimientos` |
| Entregar adelanto | 66 | `registrarAdelanto()` → `POST /api/v1/acopio/prestamos` (409 si no alcanza el saldo) |
| Filtro de estado | 76 | `cambiarFiltro()` → recarga con `?estado=` |
| Anular (adelanto) | 99 | `anularAdelanto(p)` → `PATCH .../prestamos/{id}/anular` |
| Anular (movimiento) | 124 | `anularMovimiento(m)` → `PATCH .../caja/movimientos/{id}/anular` |

**`compra-oro.html` (pantalla principal "Operaciones del acopio")**
| Botón | Línea | Método (`compra-oro.ts`) |
|---|---|---|
| Pestañas | 11-14 | `irA(t)` (290) |
| Agregar compra | 183 | `abrirNuevaCompra()` (295) |
| + Agregar otra partida | 42 | `agregarPartida()` (279) |
| Quitar | 51 | `quitarPartida(i)` (285) |
| Checkbox de adelanto | 108 | `alternarPrestamo()` (311) |
| Revisar y guardar compra | 158 | `(ngSubmit)="revisarCompra()"` (316) → abre el modal |
| Confirmar compra (modal) | 358 | `confirmarRegistroCompra()` (339) → `POST` |
| Volver a revisar (modal) | 357 | `cancelarRegistroCompra()` (335) |
| Checkbox de partida disponible | 206 | `alternarSeleccion()` (382) |
| Checkbox "seleccionar todas" | 199 | `seleccionarColor()` (386) |
| Confirmar entrega al mayorista | 232 | `abrirConfirmacionEntrega()` (395) → abre el modal |
| Confirmar y enviar (modal) | 386 | `confirmarEnvioMayorista()` (404) → `POST` |
| Ver comprobante | 273 | `verComprobante(c)` (373) |
| Calcular precios oficiales | 317 | `calcularPrecios()` (235) |

**`comprobante-compra.ts`**
| Botón | Línea | Qué hace |
|---|---|---|
| Compartir / Descargar imagen | 62 | `compartir()`: dibuja el recibo en un `<canvas>`, genera un PNG y lo comparte (`navigator.share`) o lo descarga |
| Enviar por WhatsApp | 63 | enlace `https://wa.me/?text=...` armado por `enlaceWhatsapp()` (140) |
| Cerrar | 64 | `dialogo.close()` |

### 3.3 Signals, computed y FormArray (lo más preguntado)
- `signal(...)`: estado del componente (ej. `historial`, `seleccion`, `guardando`).
- `computed(...)`: valor derivado que se recalcula solo. En `compra-oro.ts`: `detalle` (95), `resumen` (113), `liquidacion` (129), `pesoSeleccionado` (156).
- `toSignal(...)` (líneas 86-92): convierte el `valueChanges` del formulario en signal para que `computed` lo pueda leer.
- `FormArray`: `partidas: this.fb.array([this.crearPartida()])` (línea 76). Cada partida es un `FormGroup` creado en `crearPartida()` (259-269).

---

## PARTE 4 · Recorrido completo de "Registrar una compra"

1. **Agregar compra** (`compra-oro.html:183`) → `abrirNuevaCompra()` reinicia el formulario y `registrando.set(true)` muestra el bloque "Nueva compra al minero".
2. Al **elegir el minero**, el `valueChanges` del constructor (`compra-oro.ts:165`) llama a `GET /api/v1/acopio/prestamos?idMinero=X&estado=PENDIENTE` y marca todos los adelantos.
3. Al escribir pesos, `toSignal` actualiza `partidasValue`; `detalle`, `resumen` y `liquidacion` se recalculan solos.
4. **Revisar y guardar compra** → `revisarCompra()` valida (minero, pesos, descuentos ≤ oro, motivo) y abre `<dialog #confirmacionCompra>` con `showModal()` (línea 332).
5. **Confirmar compra** → `confirmarRegistroCompra()` → `compraService.registrar()` → `POST /api/v1/acopio/compras`.
6. `CompraController.registrar` (`@Valid`) → `CompraServiceImpl.registrarCompra`:
   - busca el minero con `mineroService.obtenerEntidad` (404 si no existe),
   - crea la cabecera y cada partida, valida la merma y calcula el importe,
   - `compraRepository.saveAndFlush(compra)` guarda cabecera **y** partidas (por `cascade = PERSIST`),
   - `prestamos.descontar(...)` pasa los adelantos a DESCONTADO,
   - valida que los descuentos no superen el oro,
   - `caja.registrarPagoCompra(...)` registra el egreso del neto.
   - Si algo falla, `@Transactional` **deshace todo**.
7. Responde `201 Created` con `CompraResponse`. El frontend cierra el modal, recarga datos y hace `comprobante.set(c)`; el `effect` de `ComprobanteCompra` (línea 86) abre el comprobante.

---

## PARTE 5 · Preguntas de estudio por nivel

### NIVEL BÁSICO

**B1. ¿Dónde está el botón para registrar un minero y qué hace?**
- **Respuesta corta:** en la lista de mineros; es un enlace a `/mineros/nuevo` que abre el formulario.
- **Explicación:** no es un `(click)`; usa `routerLink`, así Angular cambia de ruta sin recargar. La ruta carga `MineroForm` con *lazy loading*.
- **Archivo:** `Frontend/src/app/acopio/minero/minero-list.html` línea 10 · `Frontend/src/app/app.routes.ts` línea 22
- **Clase/método:** `MineroList` (enlace) → `MineroForm`
- **Código:**
  ```html
  <a routerLink="/mineros/nuevo" class="primary">Registrar minero</a>
  ```

**B2. ¿Qué método se ejecuta al presionar "Guardar" en el formulario de minero?**
- **Respuesta corta:** `guardar()` de `MineroForm`.
- **Explicación:** el botón es `type="submit"`; el `<form>` tiene `(ngSubmit)="guardar()"`. Si el formulario es inválido marca los campos y no envía nada. Si edita usa `actualizar` (PUT) y si no `crear` (POST).
- **Archivo:** `Frontend/src/app/acopio/minero/minero-form.html` líneas 10 y 48 · `minero-form.ts` línea 120
- **Código:**
  ```ts
  const operacion = this.editando
    ? this.service.actualizar(this.id()!, request)
    : this.service.crear(request);
  ```

**B3. ¿Qué controlador recibe el registro de un minero?**
- **Respuesta corta:** `MineroController`, método `crear`, en `POST /api/v1/mineros`.
- **Explicación:** `@RestController` devuelve JSON; `@RequestMapping("/api/v1/mineros")` fija la ruta base; `@PostMapping` + `@ResponseStatus(HttpStatus.CREATED)` responde 201.
- **Archivo:** `Backend/src/main/java/pe/edu/upeu/sitraoro/acopio/minero/controller/MineroController.java` líneas 24-37
- **Código:**
  ```java
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MineroResponse crear(@Valid @RequestBody MineroRequest request) {
      return service.crear(request);
  }
  ```

**B4. ¿Qué servicio procesa el registro y qué repositorio guarda?**
- **Respuesta corta:** `MineroServiceImpl.crear` y `MineroRepository.save`.
- **Explicación:** el servicio verifica el DNI duplicado, crea la entidad `Minero`, le asigna el centro actual y la guarda. `MineroRepository` extiende `JpaRepository<Minero, Long>`, por eso tiene `save` sin escribir SQL.
- **Archivo:** `.../minero/service/MineroServiceImpl.java` líneas 26-36 · `.../minero/repository/MineroRepository.java`
- **Código:**
  ```java
  if (repository.existsByDocumentoIdentidad(request.documentoIdentidad())) {
      throw new ConflictoNegocioException("Ya existe un minero con el documento: " + ...);
  }
  ```

**B5. ¿Qué entidad representa al minero y a qué tabla apunta?**
- **Respuesta corta:** `Minero`, tabla `MINEROS`.
- **Explicación:** `@Entity` la convierte en entidad JPA; `@Table(name = "MINEROS")` la une a la tabla; cada `@Column(name=...)` une un atributo con su columna. `@Id` + `@GeneratedValue(IDENTITY)` usa la columna identidad de Oracle.
- **Archivo:** `.../minero/entity/Minero.java` líneas 15-44

**B6. ¿Qué es un DTO y para qué sirve `MineroRequest`?**
- **Respuesta corta:** es el objeto que viaja por la API; `MineroRequest` es lo que el frontend envía para crear o editar.
- **Explicación:** separa la API de la tabla: el cliente no puede mandar `idMinero` ni `fechaRegistro`. Es un `record` (inmutable) y trae las validaciones.
- **Archivo:** `.../minero/dto/MineroRequest.java` líneas 6-11
- **Código:**
  ```java
  public record MineroRequest(
      @NotBlank @Size(min = 8, max = 15) String documentoIdentidad,
      @NotBlank @Size(max = 150) String nombresApellidos, ...
  ```

**B7. ¿En qué puerto y base de datos trabaja el backend?**
- **Respuesta corta:** puerto 8082, Oracle `FREEPDB1` en `localhost:1522`, usuario `BOMERP_APP`.
- **Archivo:** `Backend/src/main/resources/application-dev.yml` líneas 2 y 6-8

**B8. ¿Qué pasa si el usuario deja vacío el nombre del minero?**
- **Respuesta corta:** el formulario no se envía y muestra "El nombre es obligatorio".
- **Explicación:** hay doble validación: en Angular (`Validators.required`, `minero-form.ts` línea 36) y en el backend (`@NotBlank` + `@Valid`). Si alguien salta el frontend, el backend responde 400 con el detalle por campo (`GlobalExceptionHandler.validacion`, línea 20).

**B9. ¿Cómo llega el frontend a la URL del backend?**
- **Respuesta corta:** con `environment.apiBaseUrl` y `HttpClient`.
- **Archivo:** `Frontend/src/environments/environment.ts` línea 2 · `minero.service.ts` línea 11
- **Código:** ``private readonly url = `${environment.apiBaseUrl}/api/v1/mineros`;``

**B10. ¿Cómo se controla el acceso al sistema?**
- **Respuesta corta:** en esta versión **no hay autenticación**: todos los endpoints son públicos.
- **Explicación:** no hay Spring Security, JWT ni Keycloak (`app.config.ts` línea 11 dice "sin interceptores"). El botón "Cerrar sesión" solo navega a `/compras` (`app.ts` línea 30). El único control es **CORS**, que limita qué páginas web pueden llamar a la API desde el navegador (`CorsConfig.java` línea 15). El centro de acopio se fija por configuración (`CentroActual`). Es la misma decisión del docente: la seguridad se pospone a una sesión posterior.

### NIVEL INTERMEDIO

**I1. ¿Cómo se autocompleta el nombre al escribir el DNI?**
- **Respuesta corta:** al tener 8 dígitos, el formulario consulta `GET /api/v1/publico/identidad/dni/{dni}` y rellena `nombresApellidos`.
- **Explicación:** `valueChanges` del DNI pasa por `distinctUntilChanged`, `debounceTime(500)` (espera a que deje de escribir) y `switchMap` (cancela la consulta anterior si cambia el DNI). El backend (`ConsultaDniController`) llama a la API de RENIEC con `RestClient` y un token Bearer, une nombre + apellido paterno + materno y responde `{ nombresApellidos }`. Si falla, responde 502 y el formulario dice que se escriba a mano.
- **Archivo:** `Frontend/src/app/acopio/minero/minero-form.ts` líneas 47-90 · `.../minero/controller/ConsultaDniController.java` líneas 47-92

**I2. ¿Qué hace `@Transactional` en `registrarCompra`?**
- **Respuesta corta:** que toda la compra se guarde completa o no se guarde nada.
- **Explicación:** si después de guardar la cabecera falla una regla (por ejemplo, descuentos mayores que el oro, línea 75), la excepción hace *rollback*: no quedan cabecera, partidas, adelantos descontados ni movimiento de caja. `readOnly = true` en los listados indica que solo se lee.
- **Archivo:** `.../compra/service/CompraServiceImpl.java` líneas 36 y 90

**I3. ¿Cómo se relacionan `CompraG2` y `TransaccionG2`?**
- **Respuesta corta:** una compra (cabecera) tiene muchas partidas (detalle): `@OneToMany` / `@ManyToOne`.
- **Explicación:** `CompraG2.detalles` es `@OneToMany(mappedBy = "compra", cascade = CascadeType.PERSIST)`; el dueño de la relación es `TransaccionG2.compra` (`@ManyToOne` + `@JoinColumn(name = "ID_COMPRA_G2")`). Por el `cascade`, guardar la cabecera guarda las partidas.
- **Archivo:** `.../compra/entity/CompraG2.java` línea 57 · `.../compra/entity/TransaccionG2.java` líneas 33-35

**I4. ¿Dónde se calcula la merma y qué pasa si es negativa?**
- **Respuesta corta:** en `CalculadorPrecioOro.calcularMermaGramos`; si el fundido supera al recibido lanza `IllegalArgumentException` → 400.
- **Explicación:** en el frontend también se avisa en vivo (`detalle().excede`) y no se abre el modal. En el backend se vuelve a validar porque nunca se confía solo en el cliente.
- **Archivo:** `.../compra/service/CalculadorPrecioOro.java` líneas 27-33 · `compra-oro.ts` líneas 95-110

**I5. ¿Por qué `CompraServiceImpl` usa `MineroService` y no `MineroRepository`?**
- **Respuesta corta:** por la regla del docente: un submódulo usa el **servicio público** del otro, no su repositorio.
- **Archivo:** `CompraServiceImpl.java` línea 29 (comentario) y 38
- **Código:** `private final MineroService mineroService; // usa el servicio publico del otro submodulo, no su repository`

**I6. ¿Qué hace `@Valid` y qué pasa si falla?**
- **Respuesta corta:** activa las anotaciones de validación del DTO; si alguna falla responde 400 con los campos.
- **Explicación:** en `CompraRequest` el `@Valid` sobre la lista también valida cada `PartidaRequest` (`@Pattern("ROJO|VERDE")`, `@DecimalMin("0.001")`...). Spring lanza `MethodArgumentNotValidException`, capturada en `GlobalExceptionHandler.validacion`.
- **Archivo:** `.../compra/dto/CompraRequest.java` línea 14 · `.../compra/dto/PartidaRequest.java` líneas 11-14 · `GlobalExceptionHandler.java` línea 19

**I7. ¿Cómo se calcula en pantalla el total a pagar al minero?**
- **Respuesta corta:** con el `computed` `liquidacion`: valor del oro − adelantos marcados − otro descuento.
- **Explicación:** `toSignal` convierte los cambios del formulario en signals (líneas 86-92); `liquidacion` (129) los lee y se recalcula solo. También calcula `excede` y `faltaMotivo`, que bloquean el envío en `revisarCompra()`.
- **Archivo:** `Frontend/src/app/acopio/compra/compra-oro.ts` líneas 129-142

**I8. ¿Qué pasa al presionar "Eliminar" en un minero con compras?**
- **Respuesta corta:** el backend responde 409 y la lista muestra "No se puede eliminar: el minero ya tiene compras o adelantos registrados".
- **Explicación:** `eliminar` hace `delete` + `flush()`; Oracle rechaza por la clave foránea, Spring lanza `DataIntegrityViolationException` y el servicio la convierte en `ConflictoNegocioException`.
- **Archivo:** `.../minero/service/MineroServiceImpl.java` líneas 63-74 · `minero-list.ts` línea 42

**I9. ¿Cómo se registra un adelanto y cómo afecta a la caja?**
- **Respuesta corta:** `POST /api/v1/acopio/prestamos`; se guarda PENDIENTE y se registra un egreso PRESTAMO en la caja.
- **Explicación:** `PrestamoMineroService.registrar` guarda con `saveAndFlush` (para tener el id) y llama a `caja.registrarPrestamo`, que lanza 409 si el saldo no alcanza. Como todo es una transacción, si la caja falla tampoco queda el adelanto.
- **Archivo:** `.../prestamo/service/PrestamoMineroService.java` líneas 43-60 · `.../caja/service/CajaAcopioService.java` líneas 96-106

**I10. ¿Para qué sirve `ddl-auto: validate`?**
- **Respuesta corta:** para que Hibernate verifique al arrancar que las entidades coinciden con las tablas, sin crearlas.
- **Explicación:** las tablas las crea el script SQL (responsabilidad de BD2). Si una columna no existe, la aplicación no arranca.
- **Archivo:** `Backend/src/main/resources/application-dev.yml` línea 13

### NIVEL AVANZADO

**A1. ¿Cómo funciona el envío al mayorista en la base de datos?**
- **Respuesta corta:** inserta una fila en `MAYORISTA_RECEPCIONES` y pone su id en `ID_RECEPCION_MAYORISTA` de cada partida; así dejan de estar disponibles.
- **Explicación:** primero bloquea las partidas (`bloquearPorIds` con `PESSIMISTIC_WRITE`), valida centro / anulada / ya enviada / liquidada (409), suma los pesos fundidos por color, inserta con `JdbcTemplate` y `KeyHolder` (para leer el id generado) y actualiza las partidas con `saveAll`.
- **Archivo:** `.../entrega/service/EntregaMayoristaService.java` líneas 64-121

**A2. ¿Por qué se usa `@Lock(LockModeType.PESSIMISTIC_WRITE)`?**
- **Respuesta corta:** para que dos usuarios no descuenten el mismo adelanto ni envíen la misma partida a la vez.
- **Explicación:** genera `SELECT ... FOR UPDATE`; la segunda transacción espera a que termine la primera y luego ve el estado nuevo (ya DESCONTADO o ya enviada) → 409.
- **Archivo:** `.../prestamo/repository/PrestamoMineroRepository.java` línea 29 · `.../compra/repository/TransaccionG2Repository.java` línea 25

**A3. ¿Qué problema resuelve `@EntityGraph` en `CompraG2Repository`?**
- **Respuesta corta:** el problema N+1: trae compras, minero y partidas en una sola consulta.
- **Explicación:** las relaciones son `LAZY`; sin `@EntityGraph`, al recorrer el historial se haría una consulta por cada compra para cargar minero y partidas. Además `open-in-view: false` obliga a cargar lo necesario dentro de la transacción.
- **Archivo:** `.../compra/repository/CompraG2Repository.java` líneas 12-14

**A4. ¿Por qué `registrarCompra` usa `saveAndFlush` y no `save`?**
- **Respuesta corta:** para que Oracle genere ya el `ID_COMPRA_G2`, que se necesita para marcar los adelantos y el movimiento de caja.
- **Archivo:** `CompraServiceImpl.java` línea 59, usado en línea 73 (`guardada.getIdCompraG2()`)

**A5. ¿Cómo se calcula el saldo de caja?**
- **Respuesta corta:** con una consulta JPQL que suma ingresos y resta egresos no anulados.
- **Archivo:** `.../caja/repository/MovimientoCajaAcopioRepository.java` líneas 27-32
- **Código:**
  ```java
  SELECT COALESCE(SUM(CASE WHEN m.ingreso = true THEN m.monto ELSE -m.monto END), 0)
  FROM MovimientoCajaAcopio m
  WHERE m.idCentroAcopio = :idCentro AND m.moneda = 'PEN' AND m.anulado = false
  ```

**A6. ¿Por qué el formulario de compra usa `toSignal` además de `FormArray`?**
- **Respuesta corta:** los formularios reactivos emiten Observables; `computed` solo reacciona a signals.
- **Archivo:** `compra-oro.ts` líneas 85-92

**A7. ¿Por qué el comprobante se abre solo al guardar una compra?**
- **Respuesta corta:** porque `ComprobanteCompra` tiene un `effect` que abre el `<dialog>` cuando su input `compra` deja de ser `null`.
- **Explicación:** `confirmarRegistroCompra` hace `this.comprobante.set(c)`; la plantilla pasa `[compra]="comprobante()"`; el `effect` llama `showModal()`. Al cerrar emite `cerrado` y el padre vuelve el signal a `null`.
- **Archivo:** `Frontend/src/app/acopio/compra/comprobante-compra.ts` líneas 81-89 · `compra-oro.ts` línea 363 (`this.comprobante.set(c); // abre el comprobante digital`)

**A8. ¿Qué verifica `ModularityTests`?**
- **Respuesta corta:** con Spring Modulith, que la estructura de módulos no tenga dependencias prohibidas.
- **Archivo:** `Backend/src/test/java/pe/edu/upeu/sitraoro/ModularityTests.java`
- **Código:** `ApplicationModules.of(SitraOroApplication.class).verify();`

**A9. ¿Por qué el envío y el reporte usan `JdbcTemplate` y no una entidad?**
- **Respuesta corta:** porque `MAYORISTA_RECEPCIONES` pertenece al módulo mayorista, que no existe en este proyecto; solo se inserta una fila y se lee el reporte.
- **Archivo:** `EntregaMayoristaService.java` líneas 99-114 (insert) y desde la línea 125 (`reporte`)

**A10. Una debilidad que el docente podría señalar**
- El backend divide por **31.0999** g/onza (`CalculadorPrecioOro.java` línea 12), mientras que el servicio copiado de mercado usa **31.1035** (`market-cotizaciones.service.ts` líneas 123 y 135). Saberlo y explicarlo demuestra dominio: son constantes distintas en dos partes del sistema y conviene unificarlas.
- El token de RENIEC está en `application-dev.yml`; en producción debería ir en una variable de entorno o gestor de secretos.
- No hay autenticación: cualquiera que alcance el puerto 8082 puede usar la API.
