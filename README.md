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
├── backend/             # API REST Spring Boot   (pendiente — hito M0)
├── frontend/            # SPA Angular            (pendiente — hito M0)
├── docker-compose.yml   # api + mysql (+ mongo)  (pendiente — hito M0)
└── README.md
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
