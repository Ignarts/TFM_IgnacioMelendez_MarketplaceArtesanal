import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminReview, AdminService } from '../../core/admin/admin.service';

@Component({
  selector: 'app-admin-reviews',
  imports: [DatePipe, RouterLink],
  template: `
    <p class="intro">Todas las reseñas publicadas. Las que han denunciado los usuarios aparecen en Reportes.</p>
    @if (message()) { <p class="success">{{ message() }}</p> }
    @if (error()) { <p class="error">{{ error() }}</p> }

    @if (reviews().length === 0) {
      <p class="muted">No hay reseñas.</p>
    } @else {
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>Producto</th><th>Autor</th><th>Nota</th><th>Comentario</th><th>Fecha</th><th></th></tr></thead>
          <tbody>
            @for (review of reviews(); track review.id) {
              <tr>
                <td><a [routerLink]="['/producto', review.productId]">{{ review.productTitle }}</a></td>
                <td>{{ review.buyerName }}</td>
                <td class="stars">{{ '★'.repeat(review.rating) }}</td>
                <td class="comment">{{ review.comment }}</td>
                <td>{{ review.createdAt | date: 'dd/MM/yyyy' }}</td>
                <td class="actions">
                  <button type="button" class="danger" (click)="remove(review)">Eliminar</button>
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
    .stars { color: var(--gold); white-space: nowrap; }
    .comment { max-width: 360px; }
  `,
})
export class AdminReviews implements OnInit {
  private adminService = inject(AdminService);

  reviews = signal<AdminReview[]>([]);
  message = signal('');
  error = signal('');

  ngOnInit(): void {
    this.adminService.listReviews().subscribe({
      next: (reviews) => this.reviews.set(reviews),
      error: () => this.error.set('Error al cargar las reseñas.'),
    });
  }

  remove(review: AdminReview): void {
    if (!confirm(`¿Eliminar la reseña de ${review.buyerName}?`)) return;
    this.adminService.deleteReview(review.id).subscribe({
      next: () => {
        this.reviews.update((list) => list.filter((r) => r.id !== review.id));
        this.error.set('');
        this.message.set(`Reseña de ${review.buyerName} eliminada.`);
      },
      error: () => this.error.set('Error al eliminar la reseña.'),
    });
  }
}
