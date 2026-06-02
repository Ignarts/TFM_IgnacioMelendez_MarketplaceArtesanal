# 00 · Visión general

## Resumen ejecutivo

**Marketplace Artesanal** es una plataforma multivendedor inspirada en Etsy a pequeña escala.
Artesanos (cerámica, joyería, cuero, ilustración, textil, madera…) publican sus productos y
los compradores los descubren, valoran y adquieren mediante un **checkout simulado**.

La diferencia clave frente a una tienda online convencional es doble:

1. **Un mismo usuario puede ejercer dos papeles**: nace como comprador y puede darse de alta
   como vendedor abriendo su propia tienda (los roles son acumulables).
2. **La confianza no la aporta una gran marca**, sino la *reputación* de cada pequeño vendedor,
   construida por software.

El proyecto gira en torno a dos ejes técnicos que lo elevan al nivel de TFM:

- La **seguridad por roles (RBAC)** con Spring Security, JWT y comprobación de propiedad de
  recursos a nivel de instancia.
- Un **sistema de reputación y confianza** entre desconocidos, con justificación teórica y
  análisis comparativo frente a plataformas reales.

## Propuesta de valor

> En una tienda de un solo dueño la confianza es implícita. En un marketplace, ¿cómo confío en
> un vendedor que no conozco? Ese es el reto que da sustancia al TFM.

La plataforma "fabrica" confianza mediante: roles bien definidos, verificación de vendedores,
valoraciones honestas (reseñas verificadas) e insignias por trayectoria.

## Stack tecnológico

| Capa | Tecnologías |
|------|-------------|
| **Frontend** | Angular · Router + Guards · Reactive Forms · RxJS · HttpClient + Interceptor JWT · SCSS/SASS |
| **Backend** | Java 17 · Spring Boot · Spring Web · Spring Security (JWT) · Spring Data JPA · Bean Validation · `@PreAuthorize` · Swagger/OpenAPI |
| **Persistencia** | MySQL (dominio transaccional) · H2 (tests) · MongoDB (eventos, opcional) |
| **Calidad y despliegue** | JUnit · Mockito · Jasmine/Karma · Docker · Docker Compose · CI básico |

## Objetivos del proyecto

- Demostrar **autorización por roles (RBAC)** con Spring Security, JWT y `@PreAuthorize`.
- Diseñar un **modelo de datos relacional** realista con relaciones 1:N y N:N.
- Construir una **SPA en Angular** con guards, resolvers, interceptores y formularios reactivos.
- Implementar un **sistema de reputación** con justificación teórica (el *twist*).
- Cubrir **tests** en backend (JUnit/Mockito) y frontend (Jasmine/Karma).
- Entregar un **despliegue reproducible** con Docker Compose y CI básico.

## Por qué esta idea como TFM

La opción se eligió frente a otras seis alternativas de e-commerce (ver
[`prototipo/Opciones-TFM-Ecommerce.html`](prototipo/Opciones-TFM-Ecommerce.html)) por ser la
de **mayor dificultad y originalidad**: obliga a usar Spring Security a fondo, a modelar datos
no triviales y a tomar decisiones de arquitectura defendibles en la memoria.

| Aspecto | Valoración |
|---------|-----------|
| Dificultad | Alta |
| Originalidad | Alta |
| Habilidad estrella | Seguridad y roles (RBAC) |
| Twist académico | Sistema de reputación y confianza |
