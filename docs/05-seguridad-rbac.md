# 05 · Seguridad y autorización (RBAC)

Este es el núcleo técnico del proyecto y donde demuestra dominio de la seguridad en Spring.
Cada endpoint declara **qué rol** puede usarlo, y los endpoints de modificación añaden además
una comprobación de **propiedad del recurso**.

## Matriz de endpoints

| Endpoint | Método | Acceso | Comprobación extra |
|----------|--------|--------|--------------------|
| `/api/products` | `GET` | PÚBLICO | — |
| `/api/auth/register` · `/api/auth/login` | `POST` | PÚBLICO | — |
| `/api/cart` · `/api/checkout` | `POST` | BUYER | — |
| `/api/me/orders` | `GET` | BUYER | Solo sus pedidos |
| `/api/products/{id}/reviews` | `POST` | BUYER | Solo si lo compró |
| `/api/me/wishlist` · `/api/me/wishlist/{productId}` | `GET` · `PUT` · `DELETE` | BUYER | Solo su lista |
| `/api/reports` | `POST` | BUYER | Un reporte abierto por usuario y objetivo |
| `/api/seller/shop` | `PUT` | SELLER | Solo su tienda |
| `/api/seller/products` | `POST` | SELLER | — |
| `/api/seller/products/{id}` | `PUT` · `DELETE` | SELLER | **Producto de su tienda** |
| `/api/seller/reviews` · `/api/seller/reviews/{id}/reply` | `GET` · `PUT` | SELLER | **Reseña de un producto suyo** |
| `/api/admin/shops/{id}/verify` | `POST` | ADMIN | — |
| `/api/admin/users/{id}/suspend` | `POST` | ADMIN | No puede suspenderse a sí mismo |
| `/api/admin/categories` · `/api/admin/categories/{id}` | `POST` · `PUT` · `DELETE` | ADMIN | No se borra una categoría en uso |
| `/api/admin/reports` · `/api/admin/reports/{id}/dismiss` | `GET` · `POST` | ADMIN | — |
| `/api/admin/products/{id}/hide` · `/restore` | `POST` | ADMIN | — |

> Los niveles de acceso son **acumulables**: un `SELLER` o un `ADMIN` también puede usar los
> endpoints de `BUYER`.

## En el backend

- `SecurityFilterChain` con un **filtro JWT propio** que valida el token y popula el
  `SecurityContext`.
- Autorización a nivel de método con `@PreAuthorize("hasRole('SELLER')")`.
- **Comprobación de propiedad en el servicio**: por ejemplo, que el producto que se intenta
  editar pertenezca a la tienda del usuario autenticado (seguridad a nivel de instancia, no
  solo de rol).
- **CORS** configurado para la SPA y **manejo global de errores** `401`/`403`.

### Ejemplo conceptual

```java
@PreAuthorize("hasRole('SELLER')")
public ProductDTO updateProduct(Long productId, ProductUpdateDTO dto, User current) {
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

    // Regla de propiedad: el producto debe ser de la tienda del usuario autenticado
    if (!product.getShop().getOwnerId().equals(current.getId())) {
        throw new AccessDeniedException("No puedes editar productos de otra tienda");
    }
    // ... aplicar cambios
}
```

## En el frontend

- **Guards** (`CanActivate`) que protegen rutas por rol.
- **Interceptor** que adjunta el JWT y redirige al login ante un `401`.
- **Menús y botones** que se muestran/ocultan según el rol del usuario.
- **Lazy loading** del módulo de vendedor y del backoffice de administración.

## Estrategia de autorización: RBAC frente a alternativas

Para la memoria conviene argumentar por qué se elige **RBAC (Role-Based Access Control)** y
cómo se complementa con comprobaciones de propiedad:

| Estrategia | Idea | Encaje en el proyecto |
|------------|------|------------------------|
| **RBAC** | Permisos asociados a roles. | Base del sistema: `BUYER`/`SELLER`/`ADMIN`. |
| **Ownership / instance-level** | El recurso pertenece al sujeto. | Complemento imprescindible (regla de propiedad). |
| **ABAC** (atributos) | Reglas sobre atributos del sujeto/recurso/entorno. | Más flexible pero sobredimensionado para este alcance. |

La combinación **RBAC + ownership** es el punto fuerte defendible: cubre el "¿qué rol tienes?"
y el "¿eres el dueño?" sin la complejidad de un motor ABAC completo.
