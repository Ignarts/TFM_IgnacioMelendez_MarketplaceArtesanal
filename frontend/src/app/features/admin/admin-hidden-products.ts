import { Component, OnInit, inject, signal } from '@angular/core';
import { LucideRotateCcw } from '@lucide/angular';
import { AdminProduct, AdminService } from '../../core/admin/admin.service';

@Component({
  selector: 'app-admin-hidden-products',
  imports: [LucideRotateCcw],
  template: `
    <p class="intro">
      Productos retirados del catálogo por moderación. Siguen visibles para su vendedor y conservan
      su historial de pedidos y reseñas.
    </p>
    @if (message()) { <p class="success">{{ message() }}</p> }
    @if (error()) { <p class="error">{{ error() }}</p> }

    @if (products().length === 0) {
      <p class="muted">No hay productos retirados.</p>
    } @else {
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>Producto</th><th>Tienda</th><th></th></tr></thead>
          <tbody>
            @for (p of products(); track p.id) {
              <tr>
                <td>{{ p.title }} <small>#{{ p.id }}</small></td>
                <td>{{ p.shopName }}</td>
                <td class="actions">
                  <button type="button" (click)="restore(p)"><svg lucideRotateCcw></svg> Restaurar</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: `
    .intro, .muted { color: var(--muted); }
    .intro { margin: 0 0 1rem; }
    .success, .error { margin-bottom: 1rem; }
    .actions button { display: inline-flex; align-items: center; gap: 0.35rem; }
    .actions svg { width: 14px; height: 14px; }
  `,
})
export class AdminHiddenProducts implements OnInit {
  private adminService = inject(AdminService);

  products = signal<AdminProduct[]>([]);
  message = signal('');
  error = signal('');

  ngOnInit(): void {
    this.adminService.listHiddenProducts().subscribe({
      next: (products) => this.products.set(products),
      error: () => this.error.set('Error al cargar los productos retirados.'),
    });
  }

  restore(product: AdminProduct): void {
    this.adminService.restoreProduct(product.id).subscribe({
      next: () => {
        this.products.update((list) => list.filter((p) => p.id !== product.id));
        this.error.set('');
        this.message.set(`"${product.title}" vuelve a estar en el catálogo.`);
      },
      error: () => this.error.set('No se pudo restaurar el producto.'),
    });
  }
}
