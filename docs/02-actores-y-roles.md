# 02 · Actores y roles

El corazón del proyecto. Cada rol ve una aplicación distinta, con endpoints y pantallas
protegidas. Un usuario nace como `COMPRADOR` y puede "darse de alta como vendedor", lo que le
añade el rol `VENDEDOR`. **Los roles son acumulables.**

## Comprador · `ROLE_BUYER`

El rol por defecto al registrarse.

- Explora el catálogo y filtra productos.
- Gestiona carrito y checkout simulado.
- Ve su histórico de pedidos.
- Deja reseñas y valoraciones de lo comprado.
- Gestiona su wishlist.

## Vendedor · `ROLE_SELLER`

Comprador que abre su tienda.

- CRUD de sus propios productos y stock.
- Panel con métricas (ventas, valoración media, reputación).
- Gestiona los pedidos que recibe.
- Responde a reseñas.
- **Solo puede tocar lo suyo** (regla de propiedad).

## Administrador · `ROLE_ADMIN`

Modera y mantiene la plataforma.

- Verifica y aprueba vendedores.
- Gestiona categorías globales.
- Modera reseñas y productos reportados.
- Suspende cuentas fraudulentas.
- Acceso total al backoffice.

## La regla de propiedad (el reto fino)

> No basta con que un usuario tenga rol `VENDEDOR` para editar un producto: debe ser **su**
> producto.

Esto obliga a aplicar seguridad **a nivel de método/instancia**, más allá del simple rol. Es
uno de los puntos más lucibles de la memoria porque demuestra que la autorización no se queda
en "¿tienes el rol?" sino que llega a "¿eres el dueño del recurso?".

Ejemplos de comprobación de propiedad:

| Acción | Comprobación extra |
|--------|--------------------|
| Editar/borrar un producto | El producto pertenece a la tienda del usuario autenticado. |
| Ver pedidos propios | El pedido es del comprador autenticado. |
| Reseñar un producto | El usuario lo compró previamente (pedido `ENTREGADO`). |
| Gestionar un pedido recibido | El pedido contiene productos de la tienda del vendedor. |

## Matriz de capacidades

| Capacidad | Comprador | Vendedor | Admin |
|-----------|:---------:|:--------:|:-----:|
| Explorar catálogo y buscar | ✅ | ✅ | ✅ |
| Carrito y checkout | ✅ | ✅ | ✅ |
| Reseñar lo comprado | ✅ | ✅ | ✅ |
| Wishlist | ✅ | ✅ | ✅ |
| Abrir tienda / CRUD productos | — | ✅ | — |
| Panel de métricas de tienda | — | ✅ | — |
| Gestionar pedidos recibidos | — | ✅ | — |
| Verificar vendedores | — | — | ✅ |
| Gestionar categorías globales | — | — | ✅ |
| Moderar reseñas / suspender cuentas | — | — | ✅ |

> Nota: el admin y el vendedor también son, técnicamente, compradores: cualquiera puede comprar.
> Los roles superiores **añaden** capacidades, no las restan.
