import { Component, OnInit, inject, signal } from '@angular/core';
import {
  LucideEyeOff,
  LucideFlag,
  LucideMessageSquare,
  LucideStore,
  LucideTags,
  LucideUsers,
} from '@lucide/angular';
import { AdminService } from '../../core/admin/admin.service';
import { AdminCategories } from './admin-categories';
import { AdminHiddenProducts } from './admin-hidden-products';
import { AdminReports } from './admin-reports';
import { AdminReviews } from './admin-reviews';
import { AdminShops } from './admin-shops';
import { AdminUsers } from './admin-users';

type AdminTab = 'reports' | 'shops' | 'users' | 'reviews' | 'hidden' | 'categories';

@Component({
  selector: 'app-admin-panel',
  imports: [
    AdminCategories,
    AdminHiddenProducts,
    AdminReports,
    AdminReviews,
    AdminShops,
    AdminUsers,
    LucideEyeOff,
    LucideFlag,
    LucideMessageSquare,
    LucideStore,
    LucideTags,
    LucideUsers,
  ],
  template: `
    <h1>Panel de administración</h1>

    <div class="tabs" role="tablist" aria-label="Secciones del panel">
      <button type="button" role="tab" class="tab" [class.active]="tab() === 'reports'"
              [attr.aria-selected]="tab() === 'reports'" (click)="tab.set('reports')">
        <svg lucideFlag></svg> Reportes
        @if (openReports()) { <span class="count">{{ openReports() }}</span> }
      </button>
      <button type="button" role="tab" class="tab" [class.active]="tab() === 'shops'"
              [attr.aria-selected]="tab() === 'shops'" (click)="tab.set('shops')">
        <svg lucideStore></svg> Tiendas
        @if (pendingShops()) { <span class="count">{{ pendingShops() }}</span> }
      </button>
      <button type="button" role="tab" class="tab" [class.active]="tab() === 'users'"
              [attr.aria-selected]="tab() === 'users'" (click)="tab.set('users')">
        <svg lucideUsers></svg> Usuarios
      </button>
      <button type="button" role="tab" class="tab" [class.active]="tab() === 'reviews'"
              [attr.aria-selected]="tab() === 'reviews'" (click)="tab.set('reviews')">
        <svg lucideMessageSquare></svg> Reseñas
      </button>
      <button type="button" role="tab" class="tab" [class.active]="tab() === 'hidden'"
              [attr.aria-selected]="tab() === 'hidden'" (click)="tab.set('hidden')">
        <svg lucideEyeOff></svg> Productos retirados
      </button>
      <button type="button" role="tab" class="tab" [class.active]="tab() === 'categories'"
              [attr.aria-selected]="tab() === 'categories'" (click)="tab.set('categories')">
        <svg lucideTags></svg> Categorías
      </button>
    </div>

    @switch (tab()) {
      @case ('reports') { <app-admin-reports (changed)="refreshCounts()" /> }
      @case ('shops') { <app-admin-shops (changed)="refreshCounts()" /> }
      @case ('users') { <app-admin-users /> }
      @case ('reviews') { <app-admin-reviews /> }
      @case ('hidden') { <app-admin-hidden-products /> }
      @case ('categories') { <app-admin-categories /> }
    }
  `,
  styles: `
    .tabs {
      display: flex; gap: 0.25rem; overflow-x: auto;
      border-bottom: 1px solid var(--border); margin-bottom: 1.5rem;
    }
    .tab {
      display: inline-flex; align-items: center; gap: 0.4rem; flex: 0 0 auto;
      border: none; border-bottom: 2px solid transparent; border-radius: 0;
      padding: 0.6rem 0.9rem; color: var(--muted); background: transparent;
    }
    .tab:hover { color: var(--brown-700); background: transparent; }
    .tab.active { color: var(--brown-700); border-bottom-color: var(--gold); font-weight: 600; }
    .tab svg { width: 17px; height: 17px; }
    .count {
      min-width: 1.25rem; padding: 0 0.4rem; border-radius: 999px;
      background: var(--gold); color: var(--brown-900);
      font-size: 0.72rem; font-weight: 700; line-height: 1.25rem; text-align: center;
    }
  `,
})
export class AdminPanel implements OnInit {
  private adminService = inject(AdminService);

  tab = signal<AdminTab>('reports');
  openReports = signal(0);
  pendingShops = signal(0);

  ngOnInit(): void {
    this.refreshCounts();
  }

  refreshCounts(): void {
    this.adminService.listReports().subscribe((r) => this.openReports.set(r.length));
    this.adminService.listPendingShops().subscribe((s) => this.pendingShops.set(s.length));
  }
}
