# 03 · Arquitectura

## Arquitectura de alto nivel

Arquitectura clásica de **tres capas desacopladas**: una SPA en Angular que consume una API
REST en Spring Boot, que a su vez persiste en una base de datos relacional. Una colección
NoSQL opcional almacena eventos para analítica.

```
┌─────────────────────────┐      ┌─────────────────────────┐      ┌─────────────────────────┐
│  ① Cliente · Angular SPA │      │  ② API REST · Spring Boot │      │  ③ Persistencia          │
│                          │ ⇄    │                          │ ⇄    │                          │
│  Components              │ HTTP │  Controladores           │ JDBC │  MySQL (dominio)         │
│  Router + Guards         │ JSON │   → Servicios            │      │  H2 (tests)              │
│  Reactive Forms          │      │   → Repositorios         │      │  MongoDB (eventos, opc.) │
│  RxJS                    │      │  Spring Security (JWT)   │      │                          │
│  HttpClient + Interceptor│      │  Spring Data JPA         │      │                          │
│  SCSS/SASS               │      │  Bean Validation         │      │                          │
│                          │      │  @PreAuthorize · OpenAPI │      │                          │
└─────────────────────────┘      └─────────────────────────┘      └─────────────────────────┘
```

### ① Cliente · Angular SPA

Lo que ve el usuario en el navegador: componentes, enrutado con guards por rol, formularios
reactivos, RxJS, `HttpClient` con interceptor que adjunta el JWT, y estilos en SCSS/SASS.

### ② API REST · Spring Boot

Patrón en capas **Controladores → Servicios → Repositorios**. Incluye Spring Web, Spring
Security con filtro JWT, Spring Data JPA, Bean Validation, autorización con `@PreAuthorize` y
documentación con Swagger/OpenAPI.

### ③ Persistencia

- **MySQL** para el dominio transaccional (usuarios, tiendas, productos, pedidos, reseñas).
- **H2** en memoria para los tests.
- **MongoDB** (opcional) para eventos (clics, búsquedas) que alimentan analítica y reputación.

## Flujo de una petición protegida

1. El usuario hace **login** y recibe un **JWT**.
2. Angular guarda el token y un **interceptor** lo adjunta en la cabecera de cada petición.
3. **Spring Security** valida el token, extrae los roles y aplica `@PreAuthorize`.
4. El **servicio** comprueba además la **propiedad del recurso** (p. ej. que el producto sea de
   la tienda del usuario autenticado).
5. Si todo es correcto, se ejecuta la lógica de negocio; si no, se devuelve `401`/`403`.

```
login ──▶ JWT ──▶ [interceptor adjunta token] ──▶ Spring Security (valida + roles)
      ──▶ @PreAuthorize ──▶ servicio (comprueba propiedad) ──▶ repositorio ──▶ MySQL
```

## Decisiones clave (tradeoffs)

| Decisión | Alternativa descartada | Justificación |
|----------|------------------------|---------------|
| **Monolito modular** | Microservicios | Alcance realista para un TFM y más fácil de defender. |
| **MySQL para el dominio** | Solo NoSQL | El dominio es transaccional y relacional (pedidos, stock). |
| **MongoDB solo para eventos** | Todo en MySQL | Encaja con agregaciones para analítica/reputación; persistencia políglota justificada. |
| **JWT stateless** | Sesiones de servidor | Encaja con SPA + REST; escala mejor y desacopla cliente/servidor. |

## Estructura de proyecto propuesta

Monorepo con backend y frontend separados, orquestados por Docker Compose:

```
TFM_IgnacioMelendez_MarketplaceArtesanal/
├── backend/            # API REST Spring Boot
│   ├── src/main/java/...
│   ├── src/test/java/...
│   └── pom.xml
├── frontend/           # SPA Angular
│   ├── src/app/...
│   └── package.json
├── docker-compose.yml  # api + mysql (+ mongo opcional)
├── docs/               # esta documentación
└── README.md
```

> La estructura definitiva se materializará en el hito **M0** del [roadmap](08-roadmap.md).
