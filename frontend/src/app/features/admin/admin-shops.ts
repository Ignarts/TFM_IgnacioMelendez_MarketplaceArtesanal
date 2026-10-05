import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, output, signal } from '@angular/core';
import { AdminService, AdminShop } from '../../core/admin/admin.service';

@Component({
  selector: 'app-admin-shops',
  imports: [DatePipe],
  template: `
    @if (message()) { <p class="success">{{ message() }}</p> }
    @if (error()) { <p class="error">{{ error() }}</p> }

    @if (shops().length === 0) {
      <p class="muted">No hay tiendas registradas.</p>
    } @else {
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>Tienda</th><th>Creada</th><th>Estado</th><th></th></tr></thead>
          <tbody>
            @for (shop of sortedShops(); track shop.id) {
              <tr>
                <td>{{ shop.name }} <small>#{{ shop.id }}</small></td>
                <td>{{ shop.createdAt | date: 'dd/MM/yyyy' }}</td>
                <td>
                  @if (shop.verified) { <span class="pill ok">Verificada</span> }
                  @else { <span class="pill warn">Pendiente</span> }
                </td>
                <td class="actions">
                  @if (!shop.verified) { <button (click)="verify(shop)">Verificar</button> }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: `
    .muted { color: var(--muted); }
    .success, .error { margin-bottom: 1rem; }
  `,
})
export class AdminShops implements OnInit {
  private adminService = inject(AdminService);

  changed = output<void>();

  shops = signal<AdminShop[]>([]);
  message = signal('');
  error = signal('');

  // Pending shops first: they are the ones that need action.
  sortedShops = computed(() =>
    [...this.shops()].sort((a, b) => Number(a.verified) - Number(b.verified) || b.id - a.id),
  );

  ngOnInit(): void {
    this.adminService.listShops().subscribe({
      next: (shops) => this.shops.set(shops),
      error: () => this.error.set('Error al cargar las tiendas.'),
    });
  }

  verify(shop: AdminShop): void {
    this.adminService.verifyShop(shop.id).subscribe({
      next: (updated) => {
        this.shops.update((list) => list.map((s) => (s.id === updated.id ? updated : s)));
        this.error.set('');
        this.message.set(`Tienda "${shop.name}" verificada.`);
        this.changed.emit();
      },
      error: () => this.error.set('Error al verificar la tienda.'),
    });
  }
}
