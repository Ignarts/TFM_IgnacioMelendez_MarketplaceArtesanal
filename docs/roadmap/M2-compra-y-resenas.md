# M2 · Compra y reseñas — Carrito, checkout simulado y valoraciones

> **Semana 4** del [roadmap](../08-roadmap.md). Tercer hito: sobre el catálogo de
> [M1](M1-catalogo-y-tiendas.md), se cierra el ciclo de compra (carrito → pedido → estados) y
> se habilitan las **reseñas verificadas** (solo quien compró puede valorar).
>
> **Objetivo del hito:** un comprador puede **añadir productos al carrito**, **realizar un
> checkout simulado** que genera un pedido, **consultar su histórico**, y el vendedor puede
> **ver y gestionar los pedidos** que recibe. Quien recibió un pedido entregado puede
> **dejar una reseña**.

---

## 1. Alcance de M2

### Qué entra (y qué no)

| Entra en M2 | Se deja para hitos posteriores |
|-------------|--------------------------------|
| Entidades `Order`, `OrderItem`, `Review` y persistencia. | `Reputacion`, score e insignias (M3). |
| Carrito (en cliente y/o servidor) y **checkout simulado**. | Pasarela de pago real (fuera de alcance del TFM). |
| Generación de pedido con **precio congelado** por línea. | Antifraude de reseñas avanzado (M3). |
| Ciclo de **estados del pedido** y transiciones. | Panel de métricas/reputación del vendedor (M3). |
| Histórico del comprador (solo sus pedidos). | Notificaciones por email (fuera de alcance). |
| Panel del vendedor con los pedidos que recibe. | Devoluciones/reembolsos (fuera de alcance). |
| **Reseñas verificadas** (solo si compró y recibió). | Moderación de reseñas por admin (M3). |
| Descuento de **stock** al confirmar el pedido. | — |

> **Criterio rector:** M2 reutiliza el patrón **RBAC + regla de propiedad** de M1 aplicado a un
> nuevo eje: "ver solo *mis* pedidos", "gestionar solo los pedidos de *mi* tienda", "reseñar
> solo lo que *yo* compré". El **checkout es simulado**: no hay pago real, pero sí integridad
> transaccional (stock, total, estados).

### Definición de "hecho" (Definition of Done)

M2 está cerrado cuando:

1. Un `BUYER` puede añadir productos al carrito y ver el total.
2. `POST /api/checkout` crea un `Order` con sus `OrderItem`, **congela el precio**, **descuenta
   stock** y deja el pedido en estado `PAGADO` (simulado), todo en una **transacción**.
3. Si no hay stock suficiente, el checkout **falla atómicamente** (no se crea pedido ni se
   descuenta nada).
4. `GET /api/me/orders` devuelve **solo** los pedidos del comprador autenticado.
5. El vendedor ve en su panel **solo** los pedidos que contienen productos de su tienda y puede
   **avanzar su estado** (p. ej. `PAGADO` → `ENVIADO` → `ENTREGADO`).
6. `POST /api/products/{id}/reviews` solo se permite si el comprador tiene un pedido
   `ENTREGADO` con ese producto; en caso contrario `403`.
7. La ficha de producto muestra sus reseñas y la valoración media.
8. Swagger documenta los nuevos endpoints con sus accesos.

---

## 2. Modelo de datos que se incorpora

Alineado con [04 · Modelo de datos](../04-modelo-de-datos.md). M2 añade `Order`, `OrderItem` y
`Review`.

### `Order` (pedido)

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | `Long` (PK) | |
| `buyerId` | FK → `User` | comprador (regla de propiedad). |
| `estado` | `enum` | `PENDIENTE` / `PAGADO` / `ENVIADO` / `ENTREGADO` (+ `CANCELADO` opc.). |
| `total` | `BigDecimal` | suma de las líneas (calculado al confirmar). |
| `fecha` | `Instant` | momento de creación. |

### `OrderItem` (línea de pedido)

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | `Long` (PK) | |
| `orderId` | FK → `Order` | |
| `productId` | FK → `Product` | |
| `precio` | `BigDecimal` | **precio congelado** en el momento de la compra. |
| `cantidad` | `int` | `> 0`. |

> El **precio se congela** en `OrderItem`: cambios posteriores en `Product.precio` no alteran
> pedidos históricos. Esto es un punto a destacar en la memoria (integridad histórica).

### `Review` (reseña)

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | `Long` (PK) | |
| `buyerId` | FK → `User` | autor de la reseña. |
| `productId` | FK → `Product` | producto reseñado. |
| `rating` | `int` | 1–5 (validado). |
| `comentario` | `String` | opcional. |
| `fecha` | `Instant` | |

### Relaciones

| Relación | Cardinalidad | Implementación JPA |
|----------|--------------|--------------------|
| `User` (buyer) — `Order` | 1:N | `@ManyToOne` en `Order` (FK `buyer_id`). |
| `Order` — `OrderItem` | 1:N | `@OneToMany` con cascada; `@ManyToOne` en `OrderItem`. |
| `Product` — `OrderItem` | 1:N | `@ManyToOne` en `OrderItem` (FK `product_id`). |
| `User` — `Review` | 1:N | `@ManyToOne` en `Review` (FK `buyer_id`). |
| `Product` — `Review` | 1:N | `@ManyToOne` en `Review` (FK `product_id`). |

> Restricción recomendada: **una reseña por (buyer, product)** mediante índice único, para
> evitar reseñas duplicadas del mismo comprador sobre el mismo producto.

---

## 3. Backend · nuevas piezas

### 3.1 Organización por dominio

```
com.marketplace/
├── cart/
│   ├── CartController.java           # BUYER: gestión de carrito (si es de servidor)
│   ├── CartService.java
│   └── dto/{CartDTO, CartItemRequest}.java
├── order/
│   ├── Order.java
│   ├── OrderItem.java
│   ├── OrderStatus.java              # enum de estados
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   ├── CheckoutService.java          # transacción: crea pedido, congela precio, baja stock
│   ├── OrderService.java             # consultas y transiciones de estado
│   ├── OrderController.java          # BUYER: /api/checkout, /api/me/orders
│   ├── SellerOrderController.java    # SELLER: /api/seller/orders, cambio de estado
│   └── dto/{OrderDTO, OrderItemDTO, CheckoutRequest}.java
└── review/
    ├── Review.java
    ├── ReviewRepository.java
    ├── ReviewService.java            # verificación "solo si compró y recibió"
    ├── ReviewController.java         # POST /api/products/{id}/reviews, GET reseñas
    └── dto/{ReviewCreateRequest, ReviewDTO}.java
```

### 3.2 Matriz de endpoints de M2

Subconjunto de la [matriz de seguridad](../05-seguridad-rbac.md):

| Endpoint | Método | Acceso | Comprobación extra |
|----------|--------|--------|--------------------|
| `/api/cart` | `GET`·`POST`·`DELETE` | BUYER | Solo su carrito |
| `/api/checkout` | `POST` | BUYER | Stock disponible |
| `/api/me/orders` | `GET` | BUYER | **Solo sus pedidos** |
| `/api/me/orders/{id}` | `GET` | BUYER | **Solo si es suyo** |
| `/api/seller/orders` | `GET` | SELLER | **Solo pedidos de su tienda** |
| `/api/seller/orders/{id}/status` | `PUT` | SELLER | **Pedido con productos de su tienda** |
| `/api/products/{id}/reviews` | `GET` | PÚBLICO | — |
| `/api/products/{id}/reviews` | `POST` | BUYER | **Solo si lo compró (pedido ENTREGADO)** |

### 3.3 Checkout simulado (transacción central del hito)

```
POST /api/checkout  { items: [{productId, cantidad}], ... }   (BUYER autenticado)
  @Transactional
  → para cada item:
      - cargar Product (bloqueo optimista/pesimista según necesidad)
      - verificar stock >= cantidad   → si no, lanzar excepción (rollback total)
      - crear OrderItem(precio = product.precio CONGELADO, cantidad)
      - product.stock -= cantidad
  → total = Σ (precio * cantidad)
  → crear Order(buyerId, estado = PAGADO, total, fecha = now)
  → vaciar carrito
  → 201 Created { OrderDTO }
```

Puntos clave:

- **Atomicidad** (`@Transactional`): o se confirma todo el pedido o no se toca nada (stock
  incluido). Si un solo producto no tiene stock, **rollback** completo.
- **Precio congelado** en cada `OrderItem`.
- **Concurrencia de stock**: usar **bloqueo optimista** (`@Version` en `Product`) y reintento,
  o **bloqueo pesimista** (`SELECT ... FOR UPDATE`) en checkout. Documentar la elección.
- **Pago simulado**: no hay pasarela; el pedido pasa directo a `PAGADO`. Se documenta como
  delimitación de alcance.

### 3.4 Ciclo de estados del pedido

```
   PENDIENTE ──checkout──▶ PAGADO ──vendedor──▶ ENVIADO ──vendedor──▶ ENTREGADO
        │                    │
        └──── CANCELADO ◀────┘   (opcional)
```

| Transición | Quién | Regla |
|------------|-------|-------|
| `→ PAGADO` | sistema (checkout) | Automática al confirmar. |
| `PAGADO → ENVIADO` | SELLER (dueño) | Solo si el pedido tiene productos de su tienda. |
| `ENVIADO → ENTREGADO` | SELLER (dueño) | Idem. Habilita poder reseñar. |
| `→ CANCELADO` | BUYER/SELLER/ADMIN | Opcional; repone stock (si se implementa). |

> Las transiciones se validan en el servicio: no se permite saltar estados ni retroceder. Solo
> el vendedor **dueño** de los productos del pedido puede avanzarlo (regla de propiedad
> aplicada al pedido).

### 3.5 Reseñas verificadas (el matiz de confianza)

```
POST /api/products/{id}/reviews  { rating, comentario }   (BUYER autenticado)
  → ReviewService.create(current, productId, dto)
      - ¿el usuario tiene un Order ENTREGADO que contiene este producto?
            NO → AccessDeniedException (403) "Solo puedes reseñar lo que has comprado"
      - ¿ya reseñó este producto?  SÍ → 409 Conflict
      - crear Review(buyerId, productId, rating, comentario, fecha)
  → 201 Created
```

> Esta verificación se apoya en la relación `Order`–`OrderItem`–`Product`: antes de insertar la
> `Review` se comprueba que exista un pedido `ENTREGADO` del comprador con ese producto. Es la
> pieza que da **credibilidad a las valoraciones** y alimenta la reputación en M3.

### 3.6 DTOs y agregados de lectura

- La ficha de producto (de M1) se enriquece con **valoración media** y **número de reseñas**
  (calculados con `@Query` de agregación o derivados).
- Nunca exponer entidades JPA: `OrderDTO`/`OrderItemDTO`/`ReviewDTO` de salida.

---

## 4. Frontend · Angular

### 4.1 Piezas a implementar

| Pieza | Responsabilidad |
|-------|-----------------|
| `CartService` | Estado del carrito (signal/store), añadir/quitar, total. |
| Vista `carrito` | Líneas, cantidades, total, botón de checkout. |
| Vista `checkout` | Confirmación simulada del pedido. |
| Vista `mis-pedidos` | Histórico del comprador con estados. |
| Módulo `seller` (ampliación) | Pedidos recibidos + cambio de estado. |
| Componente `reseñas` | Listar reseñas y formulario (solo si procede). |
| `buyerGuard` | Rutas de carrito/pedidos requieren sesión. |

### 4.2 Rutas

```typescript
{ path: 'carrito',     canActivate: [authGuard], loadComponent: () => import('./features/cart/carrito.component') },
{ path: 'checkout',    canActivate: [authGuard], loadComponent: () => import('./features/cart/checkout.component') },
{ path: 'mis-pedidos', canActivate: [authGuard], loadComponent: () => import('./features/orders/mis-pedidos.component') },
// dentro del módulo lazy 'vendedor':
{ path: 'pedidos',     loadComponent: () => import('./features/seller/pedidos.component') },
```

### 4.3 UX de reseñas

- El botón "Dejar reseña" solo se muestra si el usuario **compró y recibió** el producto (la UI
  lo deduce del histórico; el backend lo **garantiza**).
- Tras reseñar, se refrescan la media y el listado.

---

## 5. Pruebas de M2

| Tipo | Test | Foco |
|------|------|------|
| Unit | `CheckoutService` hace **rollback** si falta stock en un item. | Atomicidad |
| Unit | El precio del `OrderItem` no cambia tras modificar `Product.precio`. | Precio congelado |
| Unit | `ReviewService.create` lanza `403` si no hay pedido `ENTREGADO`. | Reseña verificada |
| Unit | Transición de estado inválida (`PAGADO → ENTREGADO` directo) se rechaza. | Ciclo de estados |
| Integración | `GET /api/me/orders` no devuelve pedidos de otro usuario. | Regla de propiedad |
| Integración | `PUT /api/seller/orders/{id}/status` ajeno → `403`. | Ownership en pedidos |

> El checkout transaccional y la reseña verificada son los **tests estrella** del hito.

---

## 6. Riesgos y decisiones a documentar

| Riesgo / decisión | Mitigación / nota para la memoria |
|-------------------|-----------------------------------|
| Condición de carrera en el **stock** (dos compras a la vez). | Bloqueo optimista (`@Version`) con reintento o pesimista; documentar. |
| Checkout no atómico deja stock/pedido inconsistentes. | `@Transactional` con rollback ante cualquier fallo. |
| Cambios de precio alteran pedidos antiguos. | Precio congelado en `OrderItem`. |
| Pedido con productos de **varias tiendas**. | Decidir: ¿un pedido global o uno por tienda? Documentar (en M2 se asume pedido global con líneas por producto). |
| Reseñas duplicadas / falsas. | Índice único `(buyer, product)` + verificación de compra entregada. |
| Pago real fuera de alcance. | Checkout **simulado** declarado explícitamente como delimitación. |
| Cancelación y reposición de stock. | Opcional; si no se implementa, dejarlo fuera de alcance documentado. |

---

## 7. Checklist de cierre de M2

- [ ] Entidades `Order`, `OrderItem`, `Review` con relaciones persisten en MySQL.
- [ ] Carrito funcional (añadir/quitar/total).
- [ ] `POST /api/checkout` transaccional: crea pedido, congela precio, descuenta stock.
- [ ] Checkout falla atómicamente sin stock suficiente.
- [ ] Ciclo de estados con transiciones válidas controladas.
- [ ] `GET /api/me/orders` devuelve solo los pedidos del comprador.
- [ ] Panel del vendedor muestra solo sus pedidos y permite avanzar estado.
- [ ] Reseña permitida **solo** con pedido `ENTREGADO` del producto.
- [ ] Una reseña por comprador y producto (índice único).
- [ ] Ficha de producto muestra media y nº de reseñas.
- [ ] Swagger documenta los endpoints de M2.
- [ ] Tests de checkout, estados y reseña verificada en verde.

---

## 8. Lo que M2 deja preparado para M3

- `Order` `ENTREGADO` + `Review` → **insumos directos del sistema de reputación** (ventas
  completadas y valoraciones).
- La verificación "solo compradores reseñan" → base de la **confianza** que el score formaliza.
- Eventos de venta/reseña → candidatos a alimentar el **Aggregation Pipeline de MongoDB** (M3).
- Datos suficientes para calcular `nVentas`, `ratingMedio` y derivar `score` e insignias.

> Siguiente hito: [M3 · Twist + calidad](../08-roadmap.md#m3--twist--calidad--semanas-5-6).
