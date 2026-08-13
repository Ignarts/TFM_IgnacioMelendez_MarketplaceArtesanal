# 07 · Diseño de la interfaz

Los mockups son de baja-media fidelidad: sirven para hacerse una idea del producto, no son el
diseño final. Mantienen la identidad **arena + dorado** y muestran las pantallas más
representativas.

> 🖐️ Hay un **prototipo interactivo** navegable en
> [`prototipo/Marketplace-Prototipo.html`](prototipo/Marketplace-Prototipo.html): permite añadir
> al carrito, guardar favoritos, filtrar, valorar con estrellas y cambiar el estado de pedidos.
> Solo visual, sin backend.

## Pantallas clave

### 1 · Marketplace (explorar)
Barra de búsqueda, filtros por categoría/precio/valoración, y rejilla de productos con vendedor,
precio y rating. El check "✓" marca tiendas verificadas. Las pills de categoría filtran la
rejilla en vivo y el corazón ♡ guarda en favoritos.

### 2 · Ficha de producto
Galería con miniaturas, datos del producto, stock y —lo importante— la **tarjeta del vendedor
con su reputación e insignias**. Las reseñas marcan "compra verificada". Incluye selector de
cantidad, añadir al carrito, guardar y un bloque para dejar valoración con estrellas.

### 3 · Panel del vendedor
Vista exclusiva del rol `SELLER`: KPIs de su tienda (ventas, pedidos, valoración, reputación),
gestión de pedidos recibidos (marcar como enviado), listado de productos y reseñas de clientes.
El admin tendría un backoffice equivalente pero global.

### 4 · Carrito
Edición de cantidades, eliminación de productos y recálculo del total en vivo. Envío gratis a
partir de cierto importe. Checkout simulado.

### 5 · Acceso (login / registro)
Alterna entre iniciar sesión y registrarse. Al registrarse, el usuario elige si quiere empezar
solo como comprador o también como vendedor (rol acumulable).

### 6 · Backoffice de administración
Vista del rol `ADMIN`: lista de tiendas pendientes de verificación con acción de verificar, que
otorga la insignia ✓ Verificado visible en el marketplace.

## Identidad visual

| Elemento | Decisión |
|----------|----------|
| **Paleta** | Arena/marrón (`#6b4423`, `#8a5a2f`, `#a97c4f`) + acento dorado (`#b9883b`). |
| **Tipografía** | Serif (Georgia) para títulos, sans-serif del sistema para interfaz. |
| **Tono** | Artesanía cuidada y cercana. |

## Responsive (mobile-first)

- La rejilla de productos pasa a una columna.
- Los filtros se colapsan.
- La barra lateral del panel del vendedor se reduce a iconos.

## Componentes Angular reutilizables

Cada bloque del prototipo se traduce en un componente con `@Input`:

- **Card de producto** (rejilla del marketplace).
- **Tarjeta de vendedor** (reputación + insignias).
- **Tabla de pedidos** (panel del vendedor y admin).
- **Bloque de reseñas** (ficha de producto y panel del vendedor).
- **KPIs** (panel del vendedor).
