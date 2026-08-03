import { Component, OnInit, inject, signal } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { AdminService, AdminShop, AdminUser } from '../../core/admin/admin.service';

@Component({
  selector: 'app-admin-panel',
  imports: [SlicePipe],
  template: `
    <h1>Panel de administración</h1>

    <section>
      <h2>Tiendas pendientes de verificación</h2>
      @if (pendingShops().length === 0) {
        <p>No hay tiendas pendientes.</p>
      } @else {
        <table>
          <thead><tr><th>ID</th><th>Nombre</th><th>Creada</th><th>Acción</th></tr></thead>
          <tbody>
            @for (shop of pendingShops(); track shop.id) {
              <tr>
                <td>{{ shop.id }}</td>
                <td>{{ shop.name }}</td>
                <td>{{ shop.createdAt | slice:0:10 }}</td>
                <td><button (click)="verifyShop(shop)">Verificar</button></td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>

    <section>
      <h2>Usuarios</h2>
      @if (users().length === 0) {
        <p>Cargando…</p>
      } @else {
        <table>
          <thead><tr><th>ID</th><th>Nombre</th><th>Email</th><th>Roles</th><th>Estado</th><th>Acción</th></tr></thead>
          <tbody>
            @for (user of users(); track user.id) {
              <tr>
                <td>{{ user.id }}</td>
                <td>{{ user.name }}</td>
                <td>{{ user.email }}</td>
                <td>{{ user.roles.join(', ') }}</td>
                <td>{{ user.suspended ? 'Suspendido' : 'Activo' }}</td>
                <td>
                  @if (!user.suspended) {
                    <button (click)="suspendUser(user)">Suspender</button>
                  } @else {
                    <button (click)="unsuspendUser(user)">Reactivar</button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>

    @if (message()) { <p class="success">{{ message() }}</p> }
    @if (error()) { <p class="error">{{ error() }}</p> }
  `,
})
export class AdminPanel implements OnInit {
  private adminService = inject(AdminService);

  pendingShops = signal<AdminShop[]>([]);
  users = signal<AdminUser[]>([]);
  message = signal('');
  error = signal('');

  ngOnInit(): void {
    this.loadPendingShops();
    this.loadUsers();
  }

  private loadPendingShops(): void {
    this.adminService.listPendingShops().subscribe({
      next: (shops) => this.pendingShops.set(shops),
      error: () => this.error.set('Error al cargar tiendas.'),
    });
  }

  private loadUsers(): void {
    this.adminService.listUsers().subscribe({
      next: (users) => this.users.set(users),
      error: () => this.error.set('Error al cargar usuarios.'),
    });
  }

  verifyShop(shop: AdminShop): void {
    this.adminService.verifyShop(shop.id).subscribe({
      next: () => {
        this.message.set(`Tienda "${shop.name}" verificada.`);
        this.loadPendingShops();
      },
      error: () => this.error.set('Error al verificar la tienda.'),
    });
  }

  suspendUser(user: AdminUser): void {
    this.adminService.suspendUser(user.id).subscribe({
      next: (updated) => {
        this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
        this.message.set(`Usuario "${user.name}" suspendido.`);
      },
      error: () => this.error.set('Error al suspender usuario.'),
    });
  }

  unsuspendUser(user: AdminUser): void {
    this.adminService.unsuspendUser(user.id).subscribe({
      next: (updated) => {
        this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
        this.message.set(`Usuario "${user.name}" reactivado.`);
      },
      error: () => this.error.set('Error al reactivar usuario.'),
    });
  }
}
