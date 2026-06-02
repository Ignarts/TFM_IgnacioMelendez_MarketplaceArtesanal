# M3 · Twist + calidad — Reputación, tests y memoria

> **Semanas 5-6** del [roadmap](../08-roadmap.md). Hito final: sobre el ciclo de compra de
> [M2](M2-compra-y-resenas.md), se construye el **sistema de reputación** (el *twist* académico),
> el **panel de administración**, la **batería de tests**, la **CI básica** y se redacta el
> **análisis comparativo** de la memoria.
>
> **Objetivo del hito:** cada tienda tiene un **score de reputación (0–100)** con **insignias**,
> el **admin** puede verificar vendedores y moderar, el proyecto cuenta con **tests** en backend
> y frontend ejecutados por **CI**, y la memoria incluye el **análisis comparativo** con
> plataformas reales.

---

## 1. Alcance de M3

### Qué entra (y qué no)

| Entra en M3 | Notas / fuera de alcance |
|-------------|--------------------------|
| Entidad/agregado `Reputacion` y cálculo del **score**. | Se basa en datos ya generados en M2. |
| **Insignias** y reglas de asignación. | Gamificación de la confianza. |
| Antifraude básico (patrones sospechosos). | Detección avanzada / ML queda fuera de alcance. |
| Panel de **admin**: verificar vendedores, moderar, suspender. | Recortable a lo mínimo si falta tiempo (ver §9). |
| **Tests backend** (JUnit/Mockito) y **frontend** (Jasmine/Karma). | Cobertura razonable, no exhaustiva. |
| **CI básica** (build + tests en cada push). | Despliegue continuo queda fuera de alcance. |
| **Análisis comparativo** en la memoria. | eBay, Etsy, Wallapop, Stack Overflow. |
| (Opcional) Eventos en **MongoDB** + Aggregation Pipeline. | Persistencia políglota; recortable. |

> **Criterio rector:** M3 es lo que **eleva el e-commerce a TFM**. Si el tiempo aprieta, el
> consejo del roadmap es **recortar el panel de admin a lo mínimo** (verificar vendedores) y
> concentrar el esfuerzo en el **sistema de reputación**, que es lo que da valor académico.

### Definición de "hecho" (Definition of Done)

M3 está cerrado cuando:

1. Cada tienda expone un **score 0–100** calculado con la fórmula documentada y pesos
   justificados.
2. Las **insignias** se asignan según sus condiciones y se muestran en la tienda/ficha.
3. El **admin** puede verificar un vendedor (`verificada = true`) y suspender cuentas.
4. Existe **antifraude básico** (al menos: una reseña por comprador/producto + detección de
   patrón sospechoso documentada).
5. Hay **tests** significativos en backend y frontend, y **pasan en verde**.
6. La **CI** ejecuta build + tests automáticamente en cada push/PR.
7. La memoria incluye el **marco teórico**, el **modelo propio** y el **análisis comparativo**.

---

## 2. Sistema de reputación (el *twist*)

Desarrollo de implementación de [06 · Sistema de reputación](../06-sistema-reputacion.md).

### 2.1 Agregado `Reputacion`

| Campo | Tipo | Notas |
|-------|------|-------|
| `shopId` | FK → `Shop` | relación 1:1 con la tienda. |
| `score` | `int` | 0–100, resultado de la fórmula. |
| `nVentas` | `int` | ventas completadas (pedidos `ENTREGADO`). |
| `ratingMedio` | `BigDecimal` | media de las reseñas de sus productos. |
| `antiguedadDias` | `int` | derivado de `Shop.createdAt`. |
| `tasaIncidencias` | `BigDecimal` | proporción de pedidos con incidencia/cancelación. |
| `insignias` | `Set<Insignia>` | enum (`VERIFICADO`, `DESTACADO`, `RESPUESTA_RAPIDA`, `MAS_100_VENTAS`). |
| `actualizadoEn` | `Instant` | última recalculación. |

> Es un **agregado derivado**: no contiene verdad propia, sino el resultado de calcular sobre
> `Order`/`Review`/`Shop`. Puede materializarse (tabla) o calcularse al vuelo.

### 2.2 Fórmula del score

Tal y como se defiende en la memoria (cada término normalizado a [0,1], pesos positivos suman 1.0):

```
score = 0.40 · ratingMedioNorm      // calidad percibida (reseñas)
      + 0.30 · volumenVentasNorm     // trayectoria (ventas completadas)
      + 0.20 · antiguedadNorm        // veteranía en la plataforma
      - 0.10 · tasaIncidencias       // penalización por disputas
// resultado escalado a 0-100
```

| Término | Cómo se normaliza |
|---------|-------------------|
| `ratingMedioNorm` | `ratingMedio / 5` (reseñas 1–5). |
| `volumenVentasNorm` | escala logarítmica o por umbral (p. ej. `min(nVentas/100, 1)`). |
| `antiguedadNorm` | `min(antiguedadDias / 365, 1)` (saturación al año). |
| `tasaIncidencias` | `incidencias / pedidos` ∈ [0,1] (penaliza). |

> Los pesos son **configurables** (propiedades de aplicación) y **justificados** en la memoria.
> Conviene incluir ejemplos numéricos y un caso límite (tienda nueva sin ventas → score bajo
> pero no negativo).

### 2.3 Estrategia de cálculo

Dos opciones, a justificar (Tema 10 del temario):

| Estrategia | Ventaja | Inconveniente |
|------------|---------|---------------|
| **On-read** (al consultar) | Siempre fresco, simple. | Coste por petición; recalcula de más. |
| **Tarea programada** (`@Scheduled`) | Barato en lectura, datos materializados. | Ligero desfase entre recálculos. |

> Recomendación: **tarea programada** que recalcula periódicamente (p. ej. cada hora) y
> materializa `Reputacion`, con posibilidad de recálculo on-demand tras eventos clave (venta
> entregada, nueva reseña). Documentar la decisión.

### 2.4 Insignias

| Insignia | Condición | Cuándo se evalúa |
|----------|-----------|------------------|
| **Vendedor verificado** | `Shop.verificada = true` (admin). | Al verificar. |
| **Artesano destacado** | `score > 85` sostenido. | En cada recálculo. |
| **Respuesta rápida** | Pedidos atendidos en < 24 h. | En cada recálculo. |
| **+100 ventas** | `nVentas >= 100`. | En cada recálculo. |

### 2.5 (Opcional) Eventos en MongoDB

Los eventos que alimentan la reputación (ventas, reseñas, clics) son un caso ideal para el
**Aggregation Pipeline de MongoDB** (persistencia políglota justificada en la
[arquitectura](../03-arquitectura.md)). Si el tiempo lo permite:

- Colección `events` con documentos `{tipo, shopId, productId, userId, timestamp, payload}`.
- Pipeline de agregación para derivar `nVentas`, `ratingMedio` por tienda.
- **Recortable**: si falta tiempo, el cálculo se hace solo sobre MySQL.

---

## 3. Antifraude

Desarrollo de la sección de prevención de fraude:

| Mecanismo | Implementación | Hito de origen |
|-----------|----------------|----------------|
| Reseña verificada (solo quien compró). | Verificación en `ReviewService`. | M2 (ya hecho). |
| Una reseña por comprador y producto. | Índice único `(buyer, product)`. | M2 (ya hecho). |
| Detección de patrones sospechosos. | Heurística: muchas 5★ del mismo usuario en poco tiempo → marca para revisión. | M3 |
| Moderación de reseñas reportadas. | Endpoint admin para ocultar/eliminar reseña. | M3 |
| Suspensión de cuentas fraudulentas. | Endpoint admin `users/{id}/suspend`. | M3 |

---

## 4. Panel de administración

### 4.1 Endpoints de admin

Completa la [matriz de seguridad](../05-seguridad-rbac.md):

| Endpoint | Método | Acceso | Acción |
|----------|--------|--------|--------|
| `/api/admin/shops/{id}/verify` | `POST` | ADMIN | Marca `verificada = true` (insignia verificado). |
| `/api/admin/users/{id}/suspend` | `POST` | ADMIN | Suspende una cuenta. |
| `/api/admin/reviews/{id}` | `DELETE` | ADMIN | Modera/oculta una reseña reportada. |
| `/api/admin/categories` | `POST`·`PUT`·`DELETE` | ADMIN | Gestión global de categorías (de M1). |

### 4.2 Backoffice en Angular

- Módulo `admin` con **lazy loading** y `roleGuard(['ADMIN'])`.
- Vistas: lista de vendedores pendientes de verificar, gestión de reseñas reportadas, suspensión
  de cuentas, categorías.
- **Mínimo viable** (si falta tiempo): solo la verificación de vendedores.

---

## 5. Calidad: tests

### 5.1 Backend (JUnit 5 + Mockito + H2)

| Capa | Qué se prueba | Ejemplos |
|------|---------------|----------|
| Servicios (unit) | Lógica de negocio aislada (mocks de repos). | Cálculo del score; regla de propiedad; checkout. |
| Seguridad (integración) | RBAC y `@PreAuthorize`. | `403` sin rol; endpoints públicos sin token. |
| Repositorios/JPA | Consultas y relaciones. | Filtros del explorador; reseña verificada. |
| Controladores | Contrato HTTP (`MockMvc`). | Códigos `200/201/400/401/403/409`. |

Tests **estrella** (los que se defienden en la memoria):

- Cálculo del **score** con casos conocidos (incluido el caso límite de tienda nueva).
- **Regla de propiedad** (editar producto/pedido ajeno → `403`).
- **Checkout transaccional** (rollback sin stock).
- **Reseña verificada** (solo con pedido `ENTREGADO`).

### 5.2 Frontend (Jasmine + Karma)

| Qué se prueba | Ejemplos |
|---------------|----------|
| Servicios | `AuthService` (login/logout), `CatalogService` (filtros). |
| Guards/Interceptor | Guard de rol bloquea/permite; interceptor adjunta token. |
| Componentes | Formularios reactivos (validación), render de tarjetas/ficha. |

### 5.3 Objetivo de cobertura

Cobertura **razonable y representativa**, no exhaustiva: priorizar el núcleo (seguridad,
reputación, checkout). Documentar el % alcanzado y por qué se priorizó así.

---

## 6. CI básica

Pipeline (GitHub Actions u equivalente) que en cada push/PR:

```
1. Checkout del repo.
2. Backend:  ./mvnw -B verify        (compila + ejecuta tests con H2)
3. Frontend: npm ci && npm test -- --watch=false --browsers=ChromeHeadless
4. (Opcional) build de imágenes Docker.
5. Reporte de estado en el PR (verde/rojo).
```

| Decisión | Nota |
|----------|------|
| Tests con **H2** en CI | Sin necesidad de MySQL en el runner; reproducible. |
| **ChromeHeadless** para Karma | Tests de frontend sin entorno gráfico. |
| Build de Docker opcional | Verifica que `docker compose` sigue construyendo. |

---

## 7. Memoria: análisis comparativo

Cierre académico del TFM (desarrollo de [06 §③](../06-sistema-reputacion.md)):

- **Marco teórico**: mecanismos de confianza online y reducción de la **asimetría de
  información**.
- **Modelo propio**: la fórmula del score, justificación de pesos, insignias y antifraude.
- **Análisis comparativo** con plataformas reales:

| Plataforma | Señales | Prevención de fraude |
|------------|---------|----------------------|
| **eBay** | Feedback +/−, % valoraciones. | Restricciones a nuevos vendedores. |
| **Wallapop** | Valoraciones, verificación, respuesta. | Moderación entre particulares. |
| **Etsy** | Reseñas, antigüedad, ventas. | Reseñas ligadas a compra. |
| **Stack Overflow** | Reputación por contribuciones. | Privilegios por reputación. |

- **Conclusiones**: qué aporta el modelo propio, limitaciones y líneas futuras.

---

## 8. Pruebas y verificación de M3

| Tipo | Test | Foco |
|------|------|------|
| Unit | Score con datos conocidos da el valor esperado. | Fórmula |
| Unit | Tienda nueva sin ventas → score válido (no negativo). | Caso límite |
| Unit | Insignia `+100 ventas` se asigna al alcanzar el umbral. | Insignias |
| Integración | `POST /api/admin/shops/{id}/verify` sin rol ADMIN → `403`. | RBAC admin |
| Integración | Verificar vendedor activa `verificada` e insignia. | Flujo admin |
| CI | El pipeline pasa en verde en un push limpio. | Automatización |

---

## 9. Riesgos y plan de recorte

| Riesgo | Mitigación / recorte |
|--------|----------------------|
| Falta de tiempo en la recta final. | **Recortar admin** a solo "verificar vendedores"; mantener reputación. |
| MongoDB añade complejidad. | Es **opcional**; calcular reputación solo sobre MySQL. |
| Cobertura de tests insuficiente. | Priorizar tests estrella (seguridad, score, checkout). |
| Score difícil de justificar. | Documentar pesos con ejemplos numéricos y casos límite. |
| Antifraude sobredimensionado. | Limitar a heurística simple + moderación manual del admin. |

---

## 10. Checklist de cierre de M3

- [ ] Agregado `Reputacion` y cálculo del **score** (fórmula documentada).
- [ ] **Insignias** asignadas según condiciones y mostradas en UI.
- [ ] Estrategia de cálculo decidida (`@Scheduled` y/o on-read) y justificada.
- [ ] Antifraude básico (patrón sospechoso + moderación).
- [ ] Panel de admin: verificar vendedores (mínimo), suspender, moderar reseñas.
- [ ] Backoffice Angular con `roleGuard(['ADMIN'])` y lazy loading.
- [ ] Tests backend (JUnit/Mockito) en verde.
- [ ] Tests frontend (Jasmine/Karma) en verde.
- [ ] CI ejecuta build + tests en cada push/PR.
- [ ] (Opcional) Eventos en MongoDB + Aggregation Pipeline.
- [ ] Memoria: marco teórico + modelo propio + análisis comparativo.
- [ ] Swagger documenta los endpoints de admin.

---

## 11. Cierre del proyecto

Con M3 completado, el marketplace cubre el ciclo completo y los **dos ejes técnicos** del TFM:

- **Seguridad por roles (RBAC) + regla de propiedad** — demostrada de M0 a M3.
- **Sistema de reputación y confianza** — el *twist* académico, con justificación teórica y
  análisis comparativo.

Quedan como **líneas futuras** (a mencionar en la memoria): pasarela de pago real, upload de
imágenes, notificaciones, detección de fraude con ML, despliegue continuo y escalado.

> Hito anterior: [M2 · Compra y reseñas](M2-compra-y-resenas.md) ·
> Volver al [roadmap general](../08-roadmap.md).
