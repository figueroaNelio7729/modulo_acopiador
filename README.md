# SITRA-ORO · Módulo Acopiador

Sistema para el acopiador de oro: registro de mineros (con consulta de DNI en RENIEC), caja y adelantos,
compra de oro con partidas y comprobante digital, entrega al mayorista y mercado del oro en vivo.

```
modulo_acopiador/
├── Backend/      API REST · Java 21 + Spring Boot 4.0.7 + Oracle   → http://localhost:8082
├── Frontend/     Angular (standalone, signals)                      → http://localhost:4201
└── docs/         GUIA_DEFENSA.md (guía de estudio para la defensa)
```

## Backend (`Backend/`)
Un solo módulo `acopio` dividido en submódulos, cada uno con sus capas `controller / dto / entity / repository / service`:

| Submódulo | Responsabilidad |
|---|---|
| `minero` | Alta, edición y baja de mineros · consulta de DNI |
| `compra` | Compra de oro (cabecera + partidas), precio, merma y descuentos |
| `prestamo` | Adelantos a mineros |
| `caja` | Caja del centro en soles |
| `entrega` | Envío de compras al mayorista |
| `cotizador` | Precio por gramo publicado |

Base de datos: Oracle `FREEPDB1` en `localhost:1522` (usuario `BOMERP_APP`), configurada en
`Backend/src/main/resources/application-dev.yml`.

```bash
cd Backend
JAVA_HOME="C:\Program Files\Microsoft\jdk-21.0.12.8-hotspot" ./mvnw spring-boot:run
```

Swagger: http://localhost:8082/swagger-ui.html

## Frontend (`Frontend/`)
Pantallas: **Compras de oro**, **Caja y adelantos**, **Mineros**, **Mercado del oro**.
El código de negocio está en `Frontend/src/app/acopio/` (una carpeta por módulo, igual que el backend).

```bash
cd Frontend
npx ng serve --port 4201
```

## Documentación
`docs/GUIA_DEFENSA.md`: mapa del sistema, endpoints, reglas de negocio y preguntas de estudio.
