# 06 · Sistema de reputación y confianza (el *twist*)

Este es el capítulo que convierte un e-commerce en un TFM. La pregunta de investigación:
**¿cómo genera confianza una plataforma entre dos desconocidos?** Se estudia la teoría, se
diseña un modelo propio y se compara con plataformas reales.

## ① Marco teórico

Revisión de los mecanismos de confianza online: valoraciones, reseñas, verificación de
identidad, insignias y señales de trayectoria. Cómo cada uno reduce la **asimetría de
información** entre comprador y vendedor.

## ② Modelo propio

Un **score de reputación (0–100)** por tienda, que combina:

- valoración media (calidad percibida),
- número de ventas completadas (trayectoria),
- antigüedad (veteranía en la plataforma),
- tasa de incidencias (penalización por disputas).

Los pesos son **configurables y justificados**.

### Fórmula de ejemplo (a defender en la memoria)

```
// Score de reputación de una tienda (0-100)
score = 0.40 · ratingMedioNorm     // calidad percibida (reseñas)
      + 0.30 · volumenVentasNorm    // trayectoria (ventas completadas)
      + 0.20 · antiguedadNorm       // veteranía en la plataforma
      - 0.10 · tasaIncidencias      // penalización por disputas
```

Cada término se **normaliza** a [0, 1] antes de aplicar su peso, de modo que el resultado quede
acotado en el rango 0–100. Los pesos suman 1.0 en los términos positivos; la tasa de
incidencias actúa como penalización.

## ③ Análisis comparativo

Comparar el modelo propio con los de plataformas reales:

| Plataforma | Señales que usa | Prevención de fraude |
|------------|-----------------|----------------------|
| **eBay** | Feedback positivo/negativo, % de valoraciones. | Restricciones a nuevos vendedores. |
| **Wallapop** | Valoraciones, verificación, respuesta. | Moderación y reputación entre particulares. |
| **Etsy** | Reseñas, antigüedad, ventas. | Reseñas ligadas a compra. |
| **Stack Overflow** | Reputación por contribuciones útiles. | Límites de privilegios por reputación. |

Preguntas a responder: ¿qué señales usan?, ¿cómo previenen el fraude y las reseñas falsas?,
¿qué incentivos crean?

## Insignias (gamificación de la confianza)

| Insignia | Condición |
|----------|-----------|
| **Vendedor verificado** | Identidad aprobada por un admin. |
| **Artesano destacado** | Score > 85 sostenido. |
| **Respuesta rápida** | Atiende pedidos en < 24 h. |
| **+100 ventas** | Hito de trayectoria. |

## Prevención de fraude

- Solo puede reseñar **quien ha comprado** (reseña verificada).
- Una reseña por comprador y producto.
- Detección de patrones sospechosos (muchas 5★ del mismo usuario).
- Moderación por parte del admin de reseñas reportadas.

## Conexión con el temario

- El cálculo del score puede hacerse **on-read** (al consultar) o mediante **tareas
  programadas** (`@Scheduled`).
- Los eventos que lo alimentan (ventas, reseñas) son un caso ideal para el **Aggregation
  Pipeline de MongoDB** (Tema 10).
- La verificación "solo compradores reseñan" ejercita las **transacciones y relaciones JPA**
  (Tema 7).
