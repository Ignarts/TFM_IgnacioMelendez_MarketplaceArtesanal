import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideEyeOff, LucideTrash2, LucideX } from '@lucide/angular';
import { AdminReport, AdminService } from '../../core/admin/admin.service';

@Component({
  selector: 'app-admin-reports',
  imports: [DatePipe, RouterLink, LucideEyeOff, LucideTrash2, LucideX],
  template: `
    <p class="intro">
      Contenido señalado por los usuarios. Retira el producto o elimina la reseña si incumple las normas;
      si el reporte no está justificado, descártalo.
    </p>
    @if (message()) { <p class="success">{{ message() }}</p> }
    @if (error()) { <p class="error">{{ error() }}</p> }

    @if (loading()) {
      <p class="muted">Cargando…</p>
    } @else if (reports().length === 0) {
      <p class="empty">No hay reportes pendientes.</p>
    } @else {
      <ul class="reports">
        @for (r of reports(); track r.id) {
          <li class="report">
            <header>
              <span class="pill" [class.warn]="r.type === 'PRODUCT'">
                {{ r.type === 'PRODUCT' ? 'Producto' : 'Reseña' }}
              </span>
              <a [routerLink]="['/producto', r.productId]" class="target">{{ r.productTitle }}</a>
              <small class="when">{{ r.reporterName }} · {{ r.createdAt | date: 'dd/MM/yyyy HH:mm' }}</small>
            </header>

            @if (r.type === 'REVIEW') {
              <blockquote>
                <span class="stars">{{ stars(r.reviewRating) }}</span>
                <strong>{{ r.reviewAuthor }}</strong>
                @if (r.reviewComment) { <p>{{ r.reviewComment }}</p> }
              </blockquote>
            }

            <p class="reason"><strong>Motivo:</strong> {{ r.reason }}</p>

            <footer>
              <button type="button" (click)="dismiss(r)"><svg lucideX></svg> Descartar</button>
              @if (r.type === 'PRODUCT') {
                <button class="danger" (click)="hideProduct(r)"><svg lucideEyeOff></svg> Retirar producto</button>
              } @else {
                <button class="danger" (click)="deleteReview(r)"><svg lucideTrash2></svg> Eliminar reseña</button>
              }
            </footer>
          </li>
        }
      </ul>
    }
  `,
  styles: `
    .intro, .muted { color: var(--muted); }
    .intro { margin: 0 0 1rem; }
    .success, .error { margin-bottom: 1rem; }
    .empty {
      background: var(--surface); border: 1px dashed var(--border); border-radius: var(--radius);
      padding: 1.5rem; text-align: center; color: var(--muted);
    }
    .reports { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 1rem; }
    .report {
      background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius);
      padding: 1rem 1.15rem; box-shadow: var(--shadow);
    }
    .report header { display: flex; flex-wrap: wrap; align-items: center; gap: 0.6rem; }
    .target { font-weight: 600; }
    .when { margin-left: auto; }
    blockquote {
      margin: 0.75rem 0 0; padding: 0.6rem 0.85rem;
      background: #f7f0e5; border-left: 3px solid var(--gold); border-radius: 0 8px 8px 0;
    }
    blockquote p { margin: 0.3rem 0 0; }
    .stars { color: var(--gold); letter-spacing: 2px; margin-right: 0.4rem; }
    .reason { margin: 0.75rem 0; }
    .report footer { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 0.5rem; }
    .report footer button { display: inline-flex; align-items: center; gap: 0.35rem; }
    .report footer svg { width: 16px; height: 16px; }
  `,
})
export class AdminReports implements OnInit {
  private adminService = inject(AdminService);

  /** Emitted when the open-report count changes. */
  changed = output<void>();

  reports = signal<AdminReport[]>([]);
  loading = signal(true);
  message = signal('');
  error = signal('');

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.adminService.listReports().subscribe({
      next: (reports) => {
        this.reports.set(reports);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar los reportes.');
        this.loading.set(false);
      },
    });
  }

  private done(text: string): void {
    this.error.set('');
    this.message.set(text);
    this.load();
    this.changed.emit();
  }

  stars(rating: number | null): string {
    const full = rating ?? 0;
    return '★'.repeat(full) + '☆'.repeat(5 - full);
  }

  dismiss(report: AdminReport): void {
    this.adminService.dismissReport(report.id).subscribe({
      next: () => this.done('Reporte descartado.'),
      error: () => this.error.set('No se pudo descartar el reporte.'),
    });
  }

  hideProduct(report: AdminReport): void {
    if (!confirm(`¿Retirar "${report.productTitle}" del catálogo?`)) return;
    this.adminService.hideProduct(report.productId).subscribe({
      // Hiding closes every open report of the product, so the list is reloaded.
      next: () => this.done(`"${report.productTitle}" retirado del catálogo.`),
      error: () => this.error.set('No se pudo retirar el producto.'),
    });
  }

  deleteReview(report: AdminReport): void {
    if (!confirm(`¿Eliminar la reseña de ${report.reviewAuthor}?`)) return;
    this.adminService.deleteReview(report.targetId).subscribe({
      next: () => this.done(`Reseña de ${report.reviewAuthor} eliminada.`),
      error: () => this.error.set('No se pudo eliminar la reseña.'),
    });
  }
}
