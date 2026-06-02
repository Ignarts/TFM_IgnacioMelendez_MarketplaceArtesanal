# Documentación · Marketplace Artesanal (TFM)

Esta carpeta reúne la documentación funcional y técnica del Trabajo Fin de Máster
**Marketplace de Productos Artesanales**, una plataforma multivendedor donde un mismo
usuario puede comprar y, si lo desea, abrir su propia tienda para vender piezas hechas a mano.

La documentación se ha redactado a partir de las propuestas previas recogidas en
[`prototipo/`](prototipo/), que contienen el desarrollo conceptual de la idea y un
prototipo interactivo navegable.

## Índice

| # | Documento | Contenido |
|---|-----------|-----------|
| 00 | [Visión general](00-vision-general.md) | Resumen ejecutivo, propuesta de valor y stack tecnológico. |
| 01 | [Concepto y alcance](01-concepto-y-alcance.md) | Problema, solución, objetivos y delimitación (checkout simulado). |
| 02 | [Actores y roles](02-actores-y-roles.md) | Comprador, vendedor y administrador. Roles acumulables y regla de propiedad. |
| 03 | [Arquitectura](03-arquitectura.md) | Arquitectura de tres capas, flujo de petición protegida y decisiones de diseño. |
| 04 | [Modelo de datos](04-modelo-de-datos.md) | Entidades, relaciones y diagrama del dominio. |
| 05 | [Seguridad y autorización (RBAC)](05-seguridad-rbac.md) | Matriz de endpoints, JWT, `@PreAuthorize` y seguridad a nivel de instancia. |
| 06 | [Sistema de reputación](06-sistema-reputacion.md) | El *twist* académico: marco teórico, score, insignias y antifraude. |
| 07 | [Diseño de la interfaz](07-diseno-ui.md) | Pantallas clave, identidad visual y notas de componentes. |
| 08 | [Roadmap](08-roadmap.md) | Planificación por hitos M0 → M3. |

## Prototipos de partida

La carpeta [`prototipo/`](prototipo/) contiene tres documentos HTML autónomos
(ábrelos en el navegador):

- **`Opciones-TFM-Ecommerce.html`** — análisis comparativo de las siete ideas de e-commerce
  evaluadas, con la opción 3 (Marketplace) como la elegida.
- **`Marketplace-Artesanal.html`** — desarrollo completo de la propuesta: concepto, actores,
  arquitectura, modelo de datos, seguridad, reputación, mockups y roadmap.
- **`Marketplace-Prototipo.html`** — prototipo interactivo (solo frontend, sin backend) con
  las pantallas de explorar, ficha de producto, carrito, panel de vendedor, acceso y admin.

> Estos archivos son la fuente original de la que deriva esta documentación. Se conservan
> como referencia del estado inicial de la idea.
