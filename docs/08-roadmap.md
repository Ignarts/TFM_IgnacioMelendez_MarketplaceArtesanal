# 08 · Roadmap orientativo (M0 → M3)

Una posible planificación en cuatro hitos. La idea es tener algo funcional pronto y dejar el
*twist* de reputación y los tests para el final, cuando el dominio ya es estable.

## M0 · Cimientos — Semana 1
**Setup y autenticación**

Proyecto Spring Boot + Angular, Docker Compose (api + MySQL), entidad `User`, registro/login
con JWT y guards básicos en Angular.

- [ ] Esqueleto backend (Spring Boot) y frontend (Angular).
- [ ] Docker Compose con `api` + `mysql`.
- [ ] Entidad `User` y persistencia.
- [ ] Registro y login con JWT.
- [ ] Guards básicos e interceptor JWT en Angular.

**Objetivo:** poder registrarse e iniciar sesión.

## M1 · Catálogo y tiendas — Semanas 2-3
**Productos, categorías y rol vendedor**

CRUD de productos, alta como vendedor (tienda), explorador con filtros, fichas de producto.
Aquí se materializa la seguridad por roles y la regla de propiedad.

- [ ] Entidades `Shop`, `Product`, `Category`.
- [ ] Alta de vendedor (apertura de tienda).
- [ ] CRUD de productos con **regla de propiedad**.
- [ ] Explorador con filtros y ficha de producto.
- [ ] `@PreAuthorize` y guards por rol.

## M2 · Compra y reseñas — Semana 4
**Carrito, checkout simulado y valoraciones**

Carrito, pedido con estados, histórico del comprador, panel del vendedor con sus pedidos, y
reseñas verificadas (solo quien compró).

- [ ] Entidades `Order`, `OrderItem`, `Review`.
- [ ] Carrito y checkout simulado.
- [ ] Ciclo de estados del pedido.
- [ ] Histórico del comprador y panel del vendedor.
- [ ] Reseñas verificadas.

## M3 · Twist + calidad — Semanas 5-6
**Reputación, tests y memoria**

Sistema de reputación con score e insignias, panel de admin, batería de tests, CI y redacción
del análisis comparativo.

- [ ] Entidad `Reputacion` y cálculo del score.
- [ ] Insignias y antifraude.
- [ ] Panel de admin (verificar vendedores, moderar).
- [ ] Tests backend (JUnit/Mockito) y frontend (Jasmine/Karma).
- [ ] CI básico.
- [ ] Análisis comparativo en la memoria.

## Consejo de alcance

> Valida este alcance con tu tutor antes de empezar. El marketplace es ambicioso; si vas justo
> de tiempo, una buena estrategia es **recortar el panel de admin a lo mínimo** (verificar
> vendedores) y concentrar el esfuerzo en el **sistema de reputación**, que es lo que da valor
> académico.

## Resumen visual

```
Semana:   1        2        3        4        5        6
Hito:    [M0]     [────M1────]      [M2]     [────M3────]
         setup    catálogo+tiendas  compra   reputación+tests+memoria
         + auth   + roles           +reseñas
```
