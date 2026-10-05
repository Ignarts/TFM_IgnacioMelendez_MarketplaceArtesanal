import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { AdminService, AdminUser } from '../../core/admin/admin.service';
import { AuthService } from '../../core/auth/auth.service';

const ROLE_LABELS: Record<string, string> = { BUYER: 'Comprador', SELLER: 'Vendedor', ADMIN: 'Admin' };

@Component({
  selector: 'app-admin-users',
  imports: [DatePipe],
  template: `
    <input class="search" type="search" placeholder="Buscar por nombre o email…"
           [value]="query()" (input)="query.set($any($event.target).value)" />

    @if (message()) { <p class="success">{{ message() }}</p> }
    @if (error()) { <p class="error">{{ error() }}</p> }

    <div class="table-wrap">
      <table class="data-table">
        <thead><tr><th>Usuario</th><th>Roles</th><th>Alta</th><th>Estado</th><th></th></tr></thead>
        <tbody>
          @for (user of filteredUsers(); track user.id) {
            <tr>
              <td>
                <strong>{{ user.name }}</strong><br />
                <small>{{ user.email }}</small>
              </td>
              <td>{{ roleLabels(user.roles) }}</td>
              <td>{{ user.createdAt | date: 'dd/MM/yyyy' }}</td>
              <td>
                @if (user.suspended) { <span class="pill danger">Suspendido</span> }
                @else { <span class="pill ok">Activo</span> }
              </td>
              <td class="actions">
                @if (user.id === currentUserId()) {
                  <small>Tu cuenta</small>
                } @else if (!user.suspended) {
                  <button type="button" class="danger" (click)="suspend(user)">Suspender</button>
                } @else {
                  <button type="button" (click)="unsuspend(user)">Reactivar</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="muted">Ningún usuario coincide con la búsqueda.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: `
    .search { width: 100%; max-width: 360px; margin-bottom: 1rem; }
    .success, .error { margin-bottom: 1rem; }
    .muted { color: var(--muted); }
  `,
})
export class AdminUsers implements OnInit {
  private adminService = inject(AdminService);
  private auth = inject(AuthService);

  users = signal<AdminUser[]>([]);
  query = signal('');
  message = signal('');
  error = signal('');

  currentUserId = computed(() => this.auth.user()?.id);

  filteredUsers = computed(() => {
    const q = this.query().trim().toLowerCase();
    if (!q) return this.users();
    return this.users().filter((u) => u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q));
  });

  ngOnInit(): void {
    this.adminService.listUsers().subscribe({
      next: (users) => this.users.set(users),
      error: () => this.error.set('Error al cargar los usuarios.'),
    });
  }

  roleLabels(roles: string[]): string {
    return roles.map((r) => ROLE_LABELS[r] ?? r).join(', ');
  }

  private replace(updated: AdminUser, text: string): void {
    this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
    this.error.set('');
    this.message.set(text);
  }

  suspend(user: AdminUser): void {
    if (!confirm(`¿Suspender a ${user.name}? Perderá el acceso de inmediato.`)) return;
    this.adminService.suspendUser(user.id).subscribe({
      next: (updated) => this.replace(updated, `${user.name} ha sido suspendido.`),
      error: (e) =>
        this.error.set(e.status === 409 ? 'No puedes suspender tu propia cuenta.' : 'Error al suspender el usuario.'),
    });
  }

  unsuspend(user: AdminUser): void {
    this.adminService.unsuspendUser(user.id).subscribe({
      next: (updated) => this.replace(updated, `${user.name} ha sido reactivado.`),
      error: () => this.error.set('Error al reactivar el usuario.'),
    });
  }
}
