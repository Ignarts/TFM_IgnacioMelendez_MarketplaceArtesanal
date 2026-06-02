# M1 · Catálogo y tiendas — Productos, categorías y rol vendedor

> **Semanas 2-3** del [roadmap](../08-roadmap.md). Segundo hito: sobre los cimientos de
> autenticación de [M0](M0-cimientos.md), se construye el dominio del catálogo y se materializa
> la **seguridad por roles** y la **regla de propiedad**.
>
> **Objetivo del hito:** un usuario puede **darse de alta como vendedor** (abrir su tienda),
> **publicar y gestionar sus productos** (solo los suyos), y cualquier visitante puede
> **explorar el catálogo con filtros** y abrir la **ficha de un producto**.

---

## 1. Alcance de M1

### Qué entra (y qué no)

| Entra en M1 | Se deja para hitos posteriores |
|-------------|--------------------------------|
| Entidades `Shop`, `Product`, `Category` y persistencia. | `Order`, `OrderItem`, `Review` (M2). |
| Alta de vendedor (apertura de tienda → añade rol `SELLER`). | Carrito, checkout y pedidos (M2). |
| CRUD de productos con **regla de propiedad**. | Reseñas verificadas (M2). |
| Explorador público con filtros (categoría, texto, precio). | Sistema de reputación e insignias (M3). |
| Ficha de producto pública. | Verificación de vendedor por admin (M3, aunque el flag se crea aquí). |
| `@PreAuthorize` por rol en endpoints de vendedor. | Panel de métricas del vendedor (M2/M3). |
| Guards por rol y lazy loading del módulo vendedor en Angular. | — |
| Gestión de categorías (semilla inicial; CRUD admin mínimo). | Backoffice de admin completo (M3). |

> **Criterio rector:** M1 es donde el proyecto demuestra su **habilidad estrella** (RBAC +
> ownership). No basta con tener el rol `SELLER`: editar o borrar un producto exige además que
> el producto **pertenezca a la tienda del usuario autenticado**.

### Definición de "hecho" (Definition of Done)

M1 está cerrado cuando:

1. Un `BUYER` autenticado puede **abrir una tienda** y pasa a tener también el rol `SELLER`.
2. Un `SELLER` puede **crear, editar y borrar** productos **solo de su tienda**; intentar tocar
   los de otra tienda devuelve `403`.
3. El **explorador público** (`GET /api/products`) devuelve productos con **filtros** por
   categoría, texto y rango de precio, **paginados**.
4. La **ficha de producto** (`GET /api/products/{id}`) es accesible sin autenticación.
5. Las **categorías** existen (semilla) y se pueden listar públicamente.
6. En Angular, el módulo de vendedor está protegido por **guard de rol** y se carga con
   **lazy loading**; los botones de "editar/borrar" solo aparecen al dueño.
7. Swagger documenta los nuevos endpoints con sus niveles de acceso.

---

## 2. Modelo de datos que se incorpora

Alineado con [04 · Modelo de datos](../04-modelo-de-datos.md). M1 añade tres entidades y sus
relaciones con `User`.

### `Shop` (tienda)

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | `Long` (PK) | |
| `ownerId` | FK → `User` | relación **1:1** (un usuario, una tienda). |
| `nombre` | `String` | no nulo. |
| `descripcion` | `String` | opcional. |
| `verificada` | `boolean` | por defecto `false`; la activa un admin en M3. |
| `createdAt` | `Instant` | auditoría. |

### `Product`

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | `Long` (PK) | |
| `shopId` | FK → `Shop` | a qué tienda pertenece (clave de la regla de propiedad). |
| `categoriaId` | FK → `Category` | no nulo. |
| `titulo` | `String` | no nulo. |
| `descripcion` | `String` | |
| `precio` | `BigDecimal` | `> 0`. |
| `stock` | `int` | `>= 0`. |
| `imagenes` | `List<String>` | URLs (`@ElementCollection`). |
| `activo` | `boolean` | para ocultar sin borrar (soft delete lógico). |
| `createdAt` | `Instant` | auditoría. |

### `Category`

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | `Long` (PK) | |
| `nombre` | `String` | p. ej. "Cerámica", "Joyería". |
| `slug` | `String` | único, para URLs (`ceramica`). |

### Relaciones

| Relación | Cardinalidad | Implementación JPA |
|----------|--------------|--------------------|
| `User` — `Shop` | 1:1 | `@OneToOne` (lado `Shop` con FK `owner_id`). |
| `Shop` — `Product` | 1:N | `@ManyToOne` en `Product` (FK `shop_id`). |
| `Category` — `Product` | 1:N | `@ManyToOne` en `Product` (FK `categoria_id`). |

> Las relaciones se cargan **LAZY** por defecto. Para evitar el problema **N+1** en el
> explorador, las consultas paginadas usan `JOIN FETCH` o proyecciones a DTO.

---

## 3. Backend · nuevas piezas

### 3.1 Organización por dominio

Continúa el patrón de M0 (paquete por feature):

```
com.marketplace/
├── shop/
│   ├── Shop.java
│   ├── ShopRepository.java
│   ├── ShopService.java
│   ├── ShopController.java          # POST /api/seller/shop (alta vendedor)
│   └── dto/{ShopCreateRequest, ShopDTO}.java
├── product/
│   ├── Product.java
│   ├── ProductRepository.java       # con Specifications para filtros
│   ├── ProductService.java          # regla de propiedad vive aquí
│   ├── ProductController.java        # público: GET /api/products, /{id}
│   ├── SellerProductController.java  # SELLER: POST/PUT/DELETE
│   └── dto/{ProductCreateRequest, ProductUpdateRequest, ProductDTO, ProductCardDTO}.java
├── category/
│   ├── Category.java
│   ├── CategoryRepository.java
│   ├── CategoryService.java
│   ├── CategoryController.java        # GET público; (CRUD admin mínimo)
│   └── data/CategorySeeder.java       # semilla inicial
└── common/
    └── (NotFoundException, AccessDeniedException ya en M0)
```

### 3.2 Matriz de endpoints de M1

Subconjunto de la [matriz de seguridad](../05-seguridad-rbac.md):

| Endpoint | Método | Acceso | Comprobación extra |
|----------|--------|--------|--------------------|
| `/api/products` | `GET` | PÚBLICO | — (con filtros y paginación) |
| `/api/products/{id}` | `GET` | PÚBLICO | — |
| `/api/categories` | `GET` | PÚBLICO | — |
| `/api/seller/shop` | `POST` | BUYER autenticado | No tener ya una tienda |
| `/api/seller/shop` | `GET` | SELLER | Solo su tienda |
| `/api/seller/products` | `GET` | SELLER | Solo los suyos |
| `/api/seller/products` | `POST` | SELLER | — |
| `/api/seller/products/{id}` | `PUT` | SELLER | **Producto de su tienda** |
| `/api/seller/products/{id}` | `DELETE` | SELLER | **Producto de su tienda** |
| `/api/admin/categories` | `POST`·`PUT`·`DELETE` | ADMIN | — (mínimo viable) |

### 3.3 Alta de vendedor (apertura de tienda)

Flujo clave del hito:

```
POST /api/seller/shop  { nombre, descripcion }   (usuario BUYER autenticado)
  → ShopService.openShop(currentUser, dto)
      → si el usuario YA tiene tienda → 409 Conflict
      → crea Shop(ownerId = currentUser.id, verificada = false)
      → añade rol SELLER al User y persiste
  → 201 Created { ShopDTO }
```

> Tras añadir el rol, el JWT actual del usuario **aún no lo refleja** (los roles van en el
> token). Dos opciones, a documentar en la memoria:
> 1. **Re-login / refresh del token** tras abrir la tienda (recomendado por simplicidad).
> 2. Cargar las autoridades desde BD en cada petición (más fresco, más coste).
>
> En M1 se opta por (1): tras abrir tienda, el frontend renueva la sesión.

### 3.4 La regla de propiedad (núcleo del hito)

Autorización en **dos niveles**:

1. **Nivel de rol** (`@PreAuthorize`): ¿el usuario es `SELLER`?
2. **Nivel de instancia** (en el servicio): ¿el recurso es **suyo**?

```java
@PreAuthorize("hasRole('SELLER')")
public ProductDTO updateProduct(Long productId, ProductUpdateRequest dto, User current) {
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

    // Regla de propiedad: el producto debe ser de la tienda del usuario autenticado
    if (!product.getShop().getOwnerId().equals(current.getId())) {
        throw new AccessDeniedException("No puedes editar productos de otra tienda");
    }
    // ... aplicar cambios y guardar
}
```

> Esta doble comprobación es **lo más lucible de la memoria**: demuestra que la autorización no
> se queda en "¿qué rol tienes?" sino que llega a "¿eres el dueño del recurso?".

### 3.5 Explorador con filtros y paginación

`GET /api/products` admite parámetros opcionales:

| Parámetro | Tipo | Efecto |
|-----------|------|--------|
| `q` | string | busca en `titulo`/`descripcion`. |
| `categoria` | slug | filtra por categoría. |
| `precioMin` / `precioMax` | decimal | rango de precio. |
| `page` / `size` | int | paginación (Spring `Pageable`). |
| `sort` | string | p. ej. `precio,asc`. |

Implementación con **JPA Specifications** (criterios componibles) o `@Query` dinámico. La
respuesta usa `ProductCardDTO` (datos mínimos para la tarjeta) y `Page<>` para metadatos de
paginación. Solo se listan productos `activo = true` y de tiendas existentes.

### 3.6 DTOs y validación

- **Entrada** (`ProductCreateRequest`, `ProductUpdateRequest`): `@NotBlank` título,
  `@Positive` precio, `@PositiveOrZero` stock, `@NotNull` categoría.
- **Salida**: nunca exponer la entidad JPA directamente. `ProductDTO` (ficha completa) y
  `ProductCardDTO` (listado). Evita serializar relaciones perezosas y datos sensibles.

---

## 4. Frontend · Angular

### 4.1 Piezas a implementar

| Pieza | Responsabilidad |
|-------|-----------------|
| `CatalogService` | Listar productos con filtros, obtener ficha, listar categorías. |
| Vista `explorar` | Grid de tarjetas + panel de filtros + paginación. |
| Vista `producto/:id` | Ficha de producto (pública). |
| `SellerService` | Abrir tienda, CRUD de productos del vendedor. |
| Módulo `seller` (lazy) | Panel del vendedor: listado, alta, edición de productos. |
| `roleGuard` | `CanActivateFn` parametrizado por rol (`SELLER`/`ADMIN`). |
| Directiva/`*ngIf` por rol | Mostrar/ocultar botones según rol y propiedad. |

### 4.2 Rutas (con guard de rol y lazy loading)

```typescript
export const routes: Routes = [
  { path: 'explorar',     loadComponent: () => import('./features/catalog/explorar.component') },
  { path: 'producto/:id', loadComponent: () => import('./features/catalog/producto.component') },
  {
    path: 'vendedor',
    canActivate: [roleGuard(['SELLER'])],
    loadChildren: () => import('./features/seller/seller.routes'),   // lazy module
  },
  // ... rutas de M0 (login, registro, perfil)
];
```

### 4.3 Guard por rol (reutilizable)

```typescript
export function roleGuard(roles: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (auth.hasAnyRole(roles)) return true;
    router.navigate(['/login']);
    return false;
  };
}
```

> El guard es **defensa de UX**, no de seguridad: la autorización real la impone siempre el
> backend (`@PreAuthorize` + regla de propiedad). El guard solo evita mostrar pantallas
> inalcanzables.

### 4.4 Flujo de "hazte vendedor"

1. Un `BUYER` pulsa "Abrir mi tienda" → formulario `nombre`/`descripcion`.
2. `POST /api/seller/shop` → `201`.
3. El frontend **renueva la sesión** (re-login o refresh) para obtener el rol `SELLER` en el token.
4. Aparece el acceso al panel de vendedor.

---

## 5. Categorías (semilla)

Para que el explorador tenga contenido desde el primer momento, se cargan categorías iniciales
con un `CommandLineRunner`/seeder, p. ej.: **Cerámica, Joyería, Cuero, Ilustración, Textil,
Madera**. El CRUD de admin sobre categorías es mínimo en M1 (se amplía en M3).

---

## 6. Pruebas de M1

| Tipo | Test | Foco |
|------|------|------|
| Unit | `ProductService.update` lanza `AccessDeniedException` si el producto es de otra tienda. | **Regla de propiedad** |
| Unit | `ShopService.openShop` rechaza abrir una segunda tienda (`409`). | Invariante 1:1 |
| Integración | `POST /api/seller/products` sin rol `SELLER` → `403`. | RBAC |
| Integración | `GET /api/products?categoria=ceramica&precioMax=50` filtra y pagina. | Explorador |
| Integración | `GET /api/products/{id}` es público (`200` sin token). | Acceso público |

> La regla de propiedad merece **tests específicos**: es el punto que se defiende en la memoria.

---

## 7. Riesgos y decisiones a documentar

| Riesgo / decisión | Mitigación / nota para la memoria |
|-------------------|-----------------------------------|
| El JWT no refleja el rol `SELLER` recién añadido. | Renovar token tras abrir tienda (decisión documentada en §3.3). |
| Problema **N+1** al listar productos con su tienda/categoría. | `JOIN FETCH` o proyección a DTO; paginar siempre. |
| Exponer entidades JPA filtra datos y relaciones perezosas. | Mapear siempre a DTO de salida. |
| Borrado físico de productos rompe integridad futura (pedidos en M2). | Soft delete lógico (`activo = false`) o restricción. |
| Filtros dinámicos → SQL frágil. | JPA Specifications / `Pageable`, validar parámetros. |
| Subida de imágenes real es costosa. | En M1, **URLs de imagen** (no upload de ficheros); upload queda fuera de alcance. |

---

## 8. Checklist de cierre de M1

- [ ] Entidades `Shop`, `Product`, `Category` con sus relaciones persisten en MySQL.
- [ ] Alta de vendedor operativa (añade rol `SELLER`, invariante 1:1 tienda).
- [ ] CRUD de productos del vendedor con `@PreAuthorize('SELLER')`.
- [ ] **Regla de propiedad** verificada en editar y borrar (devuelve `403` ante intrusos).
- [ ] `GET /api/products` con filtros (categoría, texto, precio) y paginación.
- [ ] `GET /api/products/{id}` y `GET /api/categories` públicos.
- [ ] Categorías semilla cargadas.
- [ ] Guard por rol y lazy loading del módulo vendedor en Angular.
- [ ] Botones de editar/borrar visibles solo para el dueño.
- [ ] Swagger documenta los endpoints de M1 con sus accesos.
- [ ] Tests de regla de propiedad y de RBAC en verde.

---

## 9. Lo que M1 deja preparado para M2

- `Product` con `precio` y `stock` → base para el **carrito y checkout** (M2).
- `Shop` como vendedor identificable → para enrutar **pedidos recibidos** al vendedor (M2).
- DTOs y patrón Controller→Service→Repository consolidados → replicables para `Order`/`Review`.
- La **regla de propiedad** ya implementada → se reutiliza el patrón para "ver solo mis
  pedidos" y "reseñar solo lo comprado" (M2).

> Siguiente hito: [M2 · Compra y reseñas](../08-roadmap.md#m2--compra-y-reseñas--semana-4).
