# Marketplace Artesanal · TFM

> Trabajo Fin de Máster · Máster Full Stack Developer (MEDAC)
> Autor: **Ignacio Meléndez**

Plataforma **multivendedor** de productos artesanales (cerámica, joyería, cuero, ilustración,
textil, madera…), tipo Etsy a pequeña escala. Un mismo usuario puede **comprar** y, si lo desea,
abrir su propia tienda para **vender**. El proyecto se articula en torno a dos ejes técnicos:
la **seguridad por roles (RBAC)** y un **sistema de reputación y confianza** entre desconocidos.

> 🛈 **Sin pagos reales.** El checkout es simulado: se genera un pedido con estado
> `PENDIENTE → PAGADO → ENVIADO → ENTREGADO`. Toda la complejidad interesante (roles, stock,
> reputación, pedidos) se mantiene; solo se omite la pasarela de pago real.

## ✨ Características principales

- **Roles acumulables**: comprador → vendedor → admin (`ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`).
- **Autorización RBAC** con Spring Security + JWT y comprobación de **propiedad de recursos**.
- **Catálogo** con categorías, filtros, búsqueda y fichas de producto.
- **Carrito y checkout simulado** con ciclo de estados de pedido.
- **Reseñas verificadas** (solo reseña quien ha comprado).
- **Sistema de reputación** por tienda (score 0–100) e insignias.
- **Paneles diferenciados** por rol: vendedor y backoffice de administración.

## 🧱 Stack tecnológico

| Capa | Tecnologías |
|------|-------------|
| **Frontend** | Angular · Guards · Reactive Forms · RxJS · Interceptor JWT · SCSS/SASS |
| **Backend** | Java 17 · Spring Boot · Spring Web · Spring Security (JWT) · Spring Data JPA · `@PreAuthorize` · OpenAPI |
| **Persistencia** | MySQL (dominio) · H2 (tests) · MongoDB (eventos, opcional) |
| **Calidad / Deploy** | JUnit · Mockito · Jasmine/Karma · Docker · Docker Compose · CI |

## 📁 Estructura del repositorio

```
.
├── docs/                # Documentación funcional y técnica (ver docs/README.md)
│   ├── 00-vision-general.md
│   ├── 01-concepto-y-alcance.md
│   ├── 02-actores-y-roles.md
│   ├── 03-arquitectura.md
│   ├── 04-modelo-de-datos.md
│   ├── 05-seguridad-rbac.md
│   ├── 06-sistema-reputacion.md
│   ├── 07-diseno-ui.md
│   ├── 08-roadmap.md
│   └── prototipo/        # HTML de partida (propuesta + prototipo interactivo)
├── backend/             # API REST Spring Boot  (Java 17 · auth JWT)
├── frontend/            # SPA Angular           (auth: login/registro/perfil)
├── docker-compose.yml   # api + mysql
├── .env.example         # plantilla de variables (copiar a .env)
└── README.md
```

## 🚀 Puesta en marcha (hito M0)

Requisitos: **Docker + Docker Compose** (backend + base de datos) y **Node 20+** (frontend).

```bash
# 1. Variables de entorno — copia la plantilla y genera un secreto JWT
cp .env.example .env
# Edita .env y pon un JWT_SECRET largo, por ejemplo:
#   openssl rand -base64 64

# 2. Backend + MySQL (desde la raíz del repo)
docker compose up --build

# 3. Frontend (en otra terminal, desde la raíz del repo)
cd frontend && npm install && npm start
```

### 🌐 URLs

Con todo levantado:

| Servicio | URL | Descripción |
|----------|-----|-------------|
| **Frontend (SPA)** | http://localhost:4200 | Aplicación Angular (login / registro / perfil). |
| **Backend (API REST)** | http://localhost:8080/api | Endpoints de la API (`/auth/register`, `/auth/login`, `/me`). |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Documentación interactiva de la API. |
| **OpenAPI (JSON)** | http://localhost:8080/v3/api-docs | Especificación OpenAPI. |
| **MySQL** | `localhost:3306` | Base de datos (credenciales en `.env`). |

> La API tarda unos segundos en estar lista tras `docker compose up` (espera a la línea
> `Started MarketplaceApplication` en el log). El frontend espera la API en `localhost:8080`.

### ✅ Verificación

- **Tests backend**: `cd backend && ./mvnw test`
- **Smoke test de la API** (registro → login → endpoint protegido):

```bash
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"email":"ana@test.com","password":"password123","nombre":"Ana"}'

TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"ana@test.com","password":"password123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')

curl localhost:8080/api/me -H "Authorization: Bearer $TOKEN"
```

## 🛍️ Catálogo y tiendas (hito M1)

Sobre la base de M0 se añaden tiendas, productos y categorías con seguridad por roles.

| Endpoint | Método | Acceso | Descripción |
|----------|--------|--------|-------------|
| `/api/categories` | `GET` | público | Listado de categorías (semilla fija). |
| `/api/products` | `GET` | público | Explorador con filtros `q`, `categoryId`, `minPrice`, `maxPrice`. |
| `/api/products/{id}` | `GET` | público | Ficha de producto. |
| `/api/seller/shop` | `POST` · `GET` | autenticado | Abrir / consultar tienda (otorga `ROLE_SELLER`). |
| `/api/seller/products` | `GET` · `POST` | `SELLER` | Listar / crear productos de mi tienda. |
| `/api/seller/products/{id}` | `PUT` · `DELETE` | `SELLER` | Editar / borrar **con regla de propiedad**. |

En el frontend: explorador público en `/`, ficha en `/producto/:id`, alta de vendedor en
`/vender` y panel de tienda en `/mi-tienda` (protegido por guard de rol `SELLER`).

```bash
# Abrir tienda y publicar un producto (reusa el $TOKEN del smoke test de M0)
curl -X POST localhost:8080/api/seller/shop -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"name":"Cerámica Ana"}'

CAT=$(curl -s localhost:8080/api/categories | sed 's/.*"id":\([0-9]*\).*/\1/')
curl -X POST localhost:8080/api/seller/products -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Vasija\",\"price\":19.90,\"stock\":5,\"categoryId\":$CAT}"

curl localhost:8080/api/products
```

## 📚 Documentación

La documentación completa está en [`docs/`](docs/README.md). Puntos de entrada recomendados:

- [Visión general](docs/00-vision-general.md)
- [Arquitectura](docs/03-arquitectura.md)
- [Modelo de datos](docs/04-modelo-de-datos.md)
- [Seguridad y RBAC](docs/05-seguridad-rbac.md)
- [Sistema de reputación](docs/06-sistema-reputacion.md)
- [Roadmap](docs/08-roadmap.md)

### Prototipos navegables

En [`docs/prototipo/`](docs/prototipo/) hay tres documentos HTML autónomos (ábrelos en el
navegador):

| Archivo | Descripción |
|---------|-------------|
| `Opciones-TFM-Ecommerce.html` | Comparativa de las siete ideas evaluadas. |
| `Marketplace-Artesanal.html` | Desarrollo completo de la propuesta elegida. |
| `Marketplace-Prototipo.html` | Prototipo interactivo (solo frontend). |

## 📄 Licencia

Proyecto académico desarrollado como Trabajo Fin de Máster. Uso educativo.
