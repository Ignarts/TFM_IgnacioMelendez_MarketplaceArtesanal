# 04 · Modelo de datos

Entidades principales del dominio y sus relaciones. La entidad `Reputacion` es la que
materializa el *twist* del proyecto (ver [06 · Sistema de reputación](06-sistema-reputacion.md)).

## Entidades

### `User`
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | PK | |
| `email` | string | único |
| `passwordHash` | string | |
| `nombre` | string | |
| `roles[]` | enum[] | acumulables: `BUYER`, `SELLER`, `ADMIN` |

### `Shop` (tienda)
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | PK | |
| `ownerId` | FK → `User` | |
| `nombre` | string | |
| `descripcion` | string | |
| `verificada` | boolean | la activa un admin |

### `Product`
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | PK | |
| `shopId` | FK → `Shop` | |
| `categoriaId` | FK → `Category` | |
| `titulo` | string | |
| `precio` | decimal | |
| `stock` | int | |
| `imagenes[]` | string[] | |

### `Category`
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | PK | |
| `nombre` | string | |
| `slug` | string | |

### `Order`
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | PK | |
| `buyerId` | FK → `User` | |
| `estado` | enum | `PENDIENTE`/`PAGADO`/`ENVIADO`/`ENTREGADO` |
| `total` | decimal | |
| `fecha` | datetime | |

### `OrderItem`
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | PK | |
| `orderId` | FK → `Order` | |
| `productId` | FK → `Product` | |
| `precio` | decimal | precio congelado en el momento de la compra |
| `cantidad` | int | |

### `Review`
| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | PK | |
| `buyerId` | FK → `User` | |
| `productId` | FK → `Product` | |
| `rating` | int | 1–5 |
| `comentario` | string | |
| `fecha` | datetime | |

### `Reputacion` ★ (agregado calculado)
| Campo | Tipo | Notas |
|-------|------|-------|
| `shopId` | FK → `Shop` | |
| `score` | int | 0–100 |
| `nVentas` | int | ventas completadas |
| `ratingMedio` | decimal | media de reseñas |
| `insignias[]` | enum[] | verificado, destacado, +100 ventas… |

## Relaciones

| Relación | Cardinalidad | Descripción |
|----------|--------------|-------------|
| `User` — `Shop` | 1:1 | Un usuario tiene como mucho una tienda (al hacerse vendedor). |
| `Shop` — `Product` | 1:N | Una tienda publica muchos productos. |
| `Category` — `Product` | 1:N | Cada producto pertenece a una categoría. |
| `User` (buyer) — `Order` | 1:N | Un comprador hace varios pedidos. |
| `Order` — `OrderItem` | 1:N | Un pedido tiene varias líneas. |
| `Product` — `OrderItem` | 1:N | Un producto aparece en muchas líneas de pedido. |
| `User` — `Review` | 1:N | Un comprador escribe varias reseñas. |
| `Product` — `Review` | 1:N | Un producto recibe varias reseñas. |
| `Shop` — `Reputacion` | 1:1 | Agregado calculado a partir de ventas y reseñas. |

## Diagrama entidad-relación (resumen)

```
                 ┌────────┐
                 │  User  │
                 └───┬────┘
            owner 1:1│        1:N (buyer)
          ┌──────────┴───────────────┐
          ▼                          ▼
      ┌────────┐                 ┌────────┐      1:N      ┌───────────┐
      │  Shop  │                 │  Order │ ────────────▶ │ OrderItem │
      └───┬────┘                 └────────┘               └─────┬─────┘
   1:1 │  │ 1:N                                                 │ N:1
       ▼  ▼                                                     ▼
 ┌──────────┐  ┌─────────┐    1:N   ┌──────────┐  N:1  ┌──────────┐
 │Reputacion│  │ Product │ ◀────────│  Review  │       │ Category │
 └──────────┘  └────┬────┘          └──────────┘       └────┬─────┘
                    │ N:1                                    │ 1:N
                    └────────────────────────────────────────┘
```

## Notas de implementación

- El precio se **congela** en `OrderItem` en el momento de la compra para que cambios
  posteriores en `Product.precio` no alteren pedidos históricos.
- `Reputacion` es un **agregado derivado**: puede calcularse *on-read* (al consultar) o mediante
  **tareas programadas**. Los eventos que lo alimentan (ventas, reseñas) son un caso ideal para
  el **Aggregation Pipeline de MongoDB**.
- La verificación "solo compradores reseñan" se apoya en relaciones y transacciones JPA: antes
  de insertar una `Review` se comprueba que exista un `Order` `ENTREGADO` del comprador con ese
  producto.
