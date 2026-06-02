# 01 · Concepto y alcance

## El problema

En una tienda de un solo dueño, la confianza es implícita: hay una marca detrás que responde.
En un **marketplace** el comprador se enfrenta a vendedores desconocidos. La pregunta central
del proyecto es: **¿cómo confío en un vendedor que no conozco?**

Este reto introduce una **asimetría de información** entre comprador y vendedor que la
plataforma debe reducir mediante mecanismos de software.

## La solución

Una plataforma que genera confianza a través de:

- **Roles bien definidos** y separación clara de capacidades.
- **Verificación de vendedores** por parte de un administrador.
- **Valoraciones honestas**: solo puede reseñar quien ha comprado (reseña verificada).
- **Insignias por trayectoria** que señalan veteranía y buen comportamiento.

## Alcance: qué entra y qué no

### Dentro del alcance

- Catálogo de productos con categorías, filtros y búsqueda.
- Registro/login con JWT y gestión de roles acumulables.
- Alta de vendedor con su tienda y CRUD de productos/stock.
- Carrito y **checkout simulado** con ciclo de estados de pedido.
- Histórico de pedidos del comprador y gestión de pedidos del vendedor.
- Reseñas y valoraciones verificadas.
- Sistema de reputación con score e insignias.
- Backoffice de administración (verificar vendedores, moderar, suspender).
- Wishlist / favoritos.

### Fuera del alcance (decisión explícita)

> **Sin pagos reales.** El checkout es simulado: se genera un pedido con estado
> `PENDIENTE → PAGADO → ENVIADO → ENTREGADO`. Toda la complejidad interesante (roles, stock,
> reputación, pedidos) se mantiene; solo se omite la pasarela de pago real.

- No hay integración con pasarelas de pago (Stripe, PayPal, etc.).
- No hay logística ni envíos reales (el coste de envío es un cálculo simulado).
- El panel de administración puede recortarse a lo mínimo (verificar vendedores) si el tiempo
  aprieta, concentrando el esfuerzo en el sistema de reputación, que es lo que aporta valor
  académico.

## Ciclo de vida de un pedido

```
PENDIENTE ──▶ PAGADO ──▶ ENVIADO ──▶ ENTREGADO
```

Cada transición es una regla de negocio en la capa de servicio. El comprador inicia el pedido
(checkout simulado), el vendedor lo marca como enviado, y el sistema lo cierra como entregado.
Los pedidos completados (`ENTREGADO`) son los que computan para la reputación del vendedor y
habilitan reseñas verificadas.

## Conexión con el temario del máster

| Funcionalidad | Tema relacionado |
|---------------|------------------|
| Relaciones y transacciones JPA, "solo compradores reseñan" | Tema 7 |
| Autorización por roles (RBAC), seguridad | Tema 8 |
| Búsqueda y rendimiento | Temas 9 / 20 |
| Eventos y agregaciones (MongoDB Aggregation Pipeline) | Tema 10 |
| Guards, resolvers, lazy loading en Angular | Tema 19 |
| Pruebas avanzadas | Tema 23 |
| SASS / diseño responsive | Tema 24 |
