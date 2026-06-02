# M0 · Cimientos — Setup y autenticación

> **Semana 1** del [roadmap](../08-roadmap.md). Primer hito del proyecto: levantar el esqueleto
> de la aplicación y resolver el registro/login con JWT.
>
> **Objetivo del hito:** un usuario puede **registrarse e iniciar sesión**, y la SPA protege
> sus rutas privadas con guards e interceptor JWT. Todo levantable con un único
> `docker compose up`.

---

## 1. Alcance de M0

### Qué entra (y qué no)

| Entra en M0 | Se deja para hitos posteriores |
|-------------|--------------------------------|
| Esqueleto backend (Spring Boot) y frontend (Angular). | Entidades `Shop`, `Product`, `Category`… (M1). |
| `docker-compose.yml` con `api` + `mysql`. | Catálogo, carrito, pedidos, reseñas (M1/M2). |
| Entidad `User` con roles acumulables y persistencia. | Sistema de reputación, insignias (M3). |
| Registro y login con emisión y validación de **JWT**. | Verificación de vendedores por admin (M3). |
| `SecurityFilterChain` + filtro JWT propio. | `@PreAuthorize` por rol fino y regla de propiedad (M1). |
| Guards básicos e interceptor JWT en Angular. | Lazy loading de módulos vendedor/admin (M1+). |
| Endpoint protegido de prueba (`/api/me`). | Batería de tests completa y CI (M3). |
| CORS y manejo global de errores `401`/`403`. | — |

> **Criterio rector:** en M0 buscamos la *infraestructura de autenticación* funcionando de
> punta a punta. La autorización fina por rol y la regla de propiedad se materializan en M1,
> pero dejamos ya el cableado (`SecurityFilterChain`, roles en el token) preparado para ellas.

### Definición de "hecho" (Definition of Done)

M0 está cerrado cuando, con el repositorio recién clonado:

1. `docker compose up --build` levanta `api` (Spring Boot) y `mysql` sin errores.
2. `POST /api/auth/register` crea un usuario (rol `BUYER` por defecto) y persiste en MySQL.
3. `POST /api/auth/login` con credenciales válidas devuelve un **JWT** firmado.
4. `GET /api/me` devuelve `401` sin token y `200` con los datos del usuario si el token es válido.
5. En Angular, registrarse e iniciar sesión funciona desde la UI; el token se guarda y el
   **interceptor** lo adjunta automáticamente.
6. Una ruta protegida (p. ej. `/perfil`) redirige al login si no hay sesión (**guard**).
7. La documentación OpenAPI (`/swagger-ui.html`) lista los endpoints de `auth` y `me`.

---

## 2. Prerrequisitos y herramientas

| Herramienta | Versión recomendada | Uso |
|-------------|--------------------|-----|
| **JDK** | Java 17 (LTS) | Compilar y ejecutar el backend. |
| **Maven** | 3.9+ (o el wrapper `./mvnw`) | Build del backend. |
| **Node.js** | 20 LTS | Toolchain de Angular. |
| **Angular CLI** | 17+ | Generar y servir la SPA. |
| **Docker + Docker Compose** | Docker 24+ / Compose v2 | Orquestar `api` + `mysql`. |
| **Git** | cualquiera reciente | Control de versiones. |

> Se versiona el **Maven Wrapper** (`mvnw`) para no depender de la instalación local de Maven.
> El JAR de la API se construye dentro de la imagen Docker (multi-stage build), así que para
> ejecutar el proyecto basta con Docker.

---

## 3. Estructura del repositorio al cerrar M0

```
TFM_IgnacioMelendez_MarketplaceArtesanal/
├── backend/
│   ├── src/main/java/com/marketplace/
│   │   ├── MarketplaceApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java          # SecurityFilterChain, CORS, PasswordEncoder
│   │   │   └── OpenApiConfig.java            # metadatos Swagger
│   │   ├── user/
│   │   │   ├── User.java                     # entidad
│   │   │   ├── Role.java                      # enum: BUYER, SELLER, ADMIN
│   │   │   ├── UserRepository.java
│   │   │   └── UserService.java
│   │   ├── auth/
│   │   │   ├── AuthController.java            # /register, /login
│   │   │   ├── AuthService.java
│   │   │   └── dto/
│   │   │       ├── RegisterRequest.java
│   │   │       ├── LoginRequest.java
│   │   │       └── AuthResponse.java          # { token, tipo, email, roles }
│   │   ├── security/
│   │   │   ├── JwtService.java                # generar/validar/parsear token
│   │   │   ├── JwtAuthenticationFilter.java   # OncePerRequestFilter
│   │   │   └── AppUserDetailsService.java
│   │   ├── me/
│   │   │   └── MeController.java              # GET /api/me (endpoint protegido de prueba)
│   │   └── common/
│   │       └── GlobalExceptionHandler.java    # @RestControllerAdvice → 401/403/400
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── application-docker.yml
│   ├── src/test/java/com/marketplace/...      # tests mínimos de M0
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/app/
│   │   ├── core/
│   │   │   ├── auth/
│   │   │   │   ├── auth.service.ts            # register/login/logout, estado de sesión
│   │   │   │   ├── auth.guard.ts              # CanActivate
│   │   │   │   ├── jwt.interceptor.ts          # adjunta Bearer token
│   │   │   │   └── token.storage.ts           # persistencia del token
│   │   │   └── models/user.model.ts
│   │   ├── features/
│   │   │   ├── auth/                           # login + registro
│   │   │   └── perfil/                         # ruta protegida de prueba
│   │   ├── app.routes.ts
│   │   └── app.config.ts                       # provideHttpClient(withInterceptors(...))
│   ├── src/environments/
│   ├── Dockerfile                              # (opcional en M0; ver §6)
│   └── package.json
├── docker-compose.yml                          # api + mysql
├── .env.example                                # variables (secret JWT, credenciales DB)
├── docs/
└── README.md
```

> El paquete raíz propuesto es `com.marketplace`. La organización es **por feature/dominio**
> (`user`, `auth`, `security`, `me`) en lugar de por capa técnica, lo que facilita añadir
> `shop`, `product`, etc. en M1 sin reestructurar.

---

## 4. Backend · Spring Boot

### 4.1 Dependencias (`pom.xml`)

| Dependencia | Para qué |
|-------------|----------|
| `spring-boot-starter-web` | API REST. |
| `spring-boot-starter-security` | Spring Security (cadena de filtros, `PasswordEncoder`). |
| `spring-boot-starter-data-jpa` | Persistencia con JPA/Hibernate. |
| `spring-boot-starter-validation` | Bean Validation en los DTO. |
| `mysql-connector-j` | Driver MySQL (runtime). |
| `com.h2database:h2` | BD en memoria para tests (scope `test`). |
| `io.jsonwebtoken:jjwt-api/impl/jackson` (0.12.x) | Generar y validar JWT. |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI / OpenAPI. |
| `spring-boot-starter-test` | JUnit 5 + Mockito (scope `test`). |
| `spring-security-test` | Utilidades de test de seguridad (scope `test`). |

### 4.2 Configuración (`application.yml`)

Puntos clave a parametrizar **por variable de entorno** (nunca hardcodear secretos):

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/marketplace}
    username: ${DB_USER:marketplace}
    password: ${DB_PASSWORD:marketplace}
  jpa:
    hibernate:
      ddl-auto: update        # M0: cómodo para iterar. En M3 migrar a Flyway/Liquibase.
    show-sql: false
    properties:
      hibernate.format_sql: true

app:
  jwt:
    secret: ${JWT_SECRET:cambia-esto-por-un-secreto-largo-de-256-bits}
    expiration-ms: ${JWT_EXPIRATION_MS:86400000}   # 24 h

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

> `ddl-auto: update` es aceptable en M0 para iterar rápido sobre el esquema. Conviene anotar en
> la memoria que en un entorno real se sustituiría por **migraciones versionadas (Flyway)**;
> esto puede dejarse como tarea de M3.

### 4.3 Entidad `User`

Alineada con el [modelo de datos](../04-modelo-de-datos.md):

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | `Long` (PK, autogenerado) | |
| `email` | `String` | **único**, no nulo, validado con `@Email`. |
| `passwordHash` | `String` | hash BCrypt, nunca se expone en respuestas. |
| `nombre` | `String` | no nulo. |
| `roles` | `Set<Role>` | enum acumulable (`@ElementCollection` + `@Enumerated(STRING)`). |
| `createdAt` | `Instant` | auditoría básica. |

```java
public enum Role { BUYER, SELLER, ADMIN }
```

> En Spring Security las autoridades por rol se nombran con el prefijo `ROLE_`. Mapear el enum
> `BUYER` → autoridad `ROLE_BUYER` al construir el `UserDetails`. Así `hasRole('BUYER')` funciona
> sin sorpresas en M1.

### 4.4 Seguridad y JWT

Componentes a implementar:

- **`PasswordEncoder`** → `BCryptPasswordEncoder` (bean en `SecurityConfig`).
- **`JwtService`** → genera el token (subject = email, claim `roles`), lo valida (firma +
  expiración) y extrae claims.
- **`JwtAuthenticationFilter`** (`OncePerRequestFilter`) → lee la cabecera
  `Authorization: Bearer <token>`, valida y popula el `SecurityContext`.
- **`SecurityConfig`** → `SecurityFilterChain` con:
  - `csrf` deshabilitado (API stateless con JWT).
  - `sessionManagement` → `STATELESS`.
  - rutas **públicas**: `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`.
  - resto: `authenticated()`.
  - registro del `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`.
  - **CORS** abierto al origen del frontend (`http://localhost:4200`).
- **`GlobalExceptionHandler`** (`@RestControllerAdvice`) → respuestas JSON coherentes para
  `401` (no autenticado), `403` (sin permiso) y `400` (validación).

#### Flujo de autenticación (M0)

```
REGISTRO
  POST /api/auth/register {email, password, nombre}
    → valida DTO → comprueba email no existente
    → passwordHash = BCrypt(password) → guarda User(roles=[BUYER])
    → 201 Created

LOGIN
  POST /api/auth/login {email, password}
    → AuthenticationManager autentica credenciales
    → JwtService.generate(user) → 200 OK { token, tipo:"Bearer", email, roles }

PETICIÓN PROTEGIDA
  GET /api/me  (Authorization: Bearer <token>)
    → JwtAuthenticationFilter valida y popula SecurityContext
    → 200 OK { id, email, nombre, roles }   |   401 si falta/expira el token
```

### 4.5 Contrato de la API (M0)

| Endpoint | Método | Acceso | Cuerpo / Respuesta |
|----------|--------|--------|--------------------|
| `/api/auth/register` | `POST` | PÚBLICO | req `{email, password, nombre}` → `201` |
| `/api/auth/login` | `POST` | PÚBLICO | req `{email, password}` → `200 {token, tipo, email, roles}` |
| `/api/me` | `GET` | AUTENTICADO | → `200 {id, email, nombre, roles}` / `401` |

> Esta tabla es el subconjunto de M0 de la [matriz de endpoints](../05-seguridad-rbac.md). Los
> endpoints de catálogo, carrito, etc. se añaden en hitos posteriores.

### 4.6 Validación (Bean Validation)

- `RegisterRequest`: `@Email`, `@NotBlank` en `nombre`, `@Size(min=8)` en `password`.
- Email duplicado → `409 Conflict` con mensaje claro (manejado en el handler global).

---

## 5. Frontend · Angular

### 5.1 Piezas a implementar

| Pieza | Responsabilidad |
|-------|-----------------|
| `AuthService` | `register()`, `login()`, `logout()`, estado de sesión (signal/`BehaviorSubject`). |
| `TokenStorage` | Guardar/leer/borrar el JWT (`localStorage` en M0). |
| `jwtInterceptor` | Adjuntar `Authorization: Bearer <token>` y redirigir al login ante `401`. |
| `authGuard` | `CanActivateFn` que bloquea rutas privadas sin sesión. |
| Vistas `login` y `registro` | Formularios **reactivos** con validación. |
| Vista `perfil` (protegida) | Consume `GET /api/me`; sirve de prueba del guard + interceptor. |

### 5.2 Rutas

```typescript
export const routes: Routes = [
  { path: 'login',    loadComponent: () => import('./features/auth/login.component') },
  { path: 'registro', loadComponent: () => import('./features/auth/registro.component') },
  { path: 'perfil',   canActivate: [authGuard],
                      loadComponent: () => import('./features/perfil/perfil.component') },
  { path: '',         redirectTo: 'login', pathMatch: 'full' },
];
```

### 5.3 Registro del interceptor (Angular standalone)

```typescript
// app.config.ts
provideHttpClient(withInterceptors([jwtInterceptor]))
```

> La URL base de la API se parametriza en `src/environments/environment.ts`
> (`apiUrl: 'http://localhost:8080/api'`) para no acoplar el código al host.

---

## 6. Docker Compose

`docker-compose.yml` con dos servicios para M0 (`mongo` queda como opcional para M3):

```yaml
services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_DATABASE: ${DB_NAME:-marketplace}
      MYSQL_USER: ${DB_USER:-marketplace}
      MYSQL_PASSWORD: ${DB_PASSWORD:-marketplace}
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD:-root}
    ports: ["3306:3306"]
    volumes: ["mysql-data:/var/lib/mysql"]
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5

  api:
    build: ./backend
    depends_on:
      mysql: { condition: service_healthy }
    environment:
      DB_URL: jdbc:mysql://mysql:3306/${DB_NAME:-marketplace}
      DB_USER: ${DB_USER:-marketplace}
      DB_PASSWORD: ${DB_PASSWORD:-marketplace}
      JWT_SECRET: ${JWT_SECRET}
    ports: ["8080:8080"]

volumes:
  mysql-data:
```

Notas:

- El `healthcheck` + `depends_on: condition: service_healthy` evita que la API arranque antes
  de que MySQL acepte conexiones.
- El **`Dockerfile` del backend** usa *multi-stage build* (etapa Maven para compilar el JAR,
  etapa JRE ligera para ejecutarlo).
- Las credenciales y el secreto JWT viven en `.env` (se versiona solo `.env.example`).
- El **frontend** en M0 puede servirse con `ng serve` en local (más ágil para iterar). Su
  contenedor (Nginx sirviendo el build) puede dejarse para más adelante.

---

## 7. Seguridad de M0: checklist de buenas prácticas

- [ ] Contraseñas almacenadas con **BCrypt**, nunca en claro.
- [ ] `passwordHash` **nunca** se serializa en respuestas (DTO de salida sin ese campo).
- [ ] **Secreto JWT** por variable de entorno, fuera del control de versiones.
- [ ] Expiración del token configurada (24 h en M0).
- [ ] API **stateless** (`SessionCreationPolicy.STATELESS`), sin cookies de sesión.
- [ ] **CORS** restringido al origen del frontend.
- [ ] Mensajes de error de login **genéricos** (no revelar si el email existe).
- [ ] `.env` y secretos en `.gitignore`.

---

## 8. Pruebas mínimas de M0

No es el hito de testing (eso es M3), pero conviene dejar un par de tests que validen el núcleo:

| Tipo | Test | Herramienta |
|------|------|-------------|
| Unit | `JwtService` genera y valida un token correctamente. | JUnit 5 |
| Unit | `AuthService.register` rechaza email duplicado. | JUnit 5 + Mockito |
| Integración | `POST /api/auth/login` con credenciales válidas → `200` + token. | `@SpringBootTest` + H2 |
| Integración | `GET /api/me` sin token → `401`. | `MockMvc` + `spring-security-test` |

> Los tests de integración usan **H2 en memoria** (perfil `test`), no MySQL, para ser
> reproducibles y rápidos.

### Pruebas manuales (smoke test)

```bash
# 1. Registro
curl -X POST localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@test.com","password":"password123","nombre":"Ana"}'

# 2. Login (guarda el token devuelto)
curl -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@test.com","password":"password123"}'

# 3. Endpoint protegido
curl localhost:8080/api/me -H "Authorization: Bearer <TOKEN>"
```

---

## 9. Riesgos y decisiones a documentar

| Riesgo / decisión | Mitigación / nota para la memoria |
|-------------------|-----------------------------------|
| `ddl-auto: update` no es apto para producción. | Documentar la decisión; migrar a Flyway en M3. |
| Token en `localStorage` (vulnerable a XSS). | Aceptable para el alcance; mencionar alternativa `HttpOnly cookie`. |
| Secreto JWT débil por defecto. | Forzar `JWT_SECRET` por entorno; documentar generación segura. |
| Desfase de arranque API ↔ MySQL. | `healthcheck` + `depends_on: service_healthy`. |
| CORS mal configurado bloquea la SPA. | Configuración explícita de origen `localhost:4200` en `SecurityConfig`. |

---

## 10. Checklist de cierre de M0

Replica y amplía la del [roadmap](../08-roadmap.md):

- [ ] Esqueleto backend (Spring Boot) compila y arranca.
- [ ] Esqueleto frontend (Angular) compila y sirve.
- [ ] `docker compose up` levanta `api` + `mysql` sin intervención manual.
- [ ] Entidad `User` con roles persiste en MySQL.
- [ ] `POST /api/auth/register` funcional (rol `BUYER` por defecto).
- [ ] `POST /api/auth/login` devuelve JWT firmado.
- [ ] `JwtAuthenticationFilter` valida el token y protege `/api/me`.
- [ ] Interceptor JWT y guard básico operativos en Angular.
- [ ] Login/registro funcionan desde la UI.
- [ ] Swagger UI accesible y documentando los endpoints de M0.
- [ ] Tests mínimos en verde.
- [ ] `README` con instrucciones de arranque (`docker compose up`).

---

## 11. Lo que M0 deja preparado para M1

- `SecurityFilterChain` y filtro JWT ya cablean **roles en el token** → en M1 solo hay que
  añadir `@PreAuthorize("hasRole('SELLER')")` en los nuevos endpoints.
- La organización por dominio (`user/`, `auth/`, …) permite añadir `shop/`, `product/`,
  `category/` sin tocar lo existente.
- El patrón **Controller → Service → Repository** y el manejo global de errores quedan como
  plantilla replicable.
- El interceptor y el guard de Angular se reutilizan; en M1 se añaden **guards por rol** y
  lazy loading del módulo de vendedor.

> Siguiente hito: [M1 · Catálogo y tiendas](../08-roadmap.md#m1--catálogo-y-tiendas--semanas-2-3).
