import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CatalogService } from '../../core/catalog/catalog.service';
import { Order, OrderService, STATUS_LABELS } from '../../core/order/order.service';

/** Per-product review draft shown inside the "valorar compra" modal. */
interface ReviewDraft {
  rating: number;
  comment: string;
  busy: boolean;
  done: boolean;
  error: string;
}

@Component({
  selector: 'app-orders',
  imports: [RouterLink, DecimalPipe, DatePipe],
  template: `
    <h1>Mis pedidos</h1>

    @if (orders().length === 0) {
      <p>Todavía no has hecho ningún pedido. <a routerLink="/">Explora productos</a>.</p>
    } @else {
      <ul class="orders">
        @for (o of orders(); track o.id) {
          <li class="order">
            <header>
              <span>Pedido #{{ o.id }} · {{ o.createdAt | date: 'dd/MM/yyyy' }}</span>
              <span class="badge" [attr.data-status]="o.status">{{ label(o.status) }}</span>
            </header>
            <ul class="lines">
              @for (i of o.items; track i.productId) {
                <li>
                  <a [routerLink]="['/producto', i.productId]">{{ i.title }}</a>
                  × {{ i.quantity }} — {{ i.price * i.quantity | number: '1.2-2' }} €
                </li>
              }
            </ul>
            <footer>
              <strong>Total: {{ o.total | number: '1.2-2' }} €</strong>
              @if (o.status === 'PENDING') {
                <button (click)="pay(o)">Pagar (simulado)</button>
              } @else if (o.status === 'PAID' || o.status === 'SHIPPED') {
                <button (click)="openConfirm(o)">Confirmar recepción</button>
              } @else if (o.status === 'DELIVERED') {
                <button class="ghost" (click)="openReview(o)">Valorar compra</button>
              }
            </footer>
          </li>
        }
      </ul>
    }

    @if (confirming(); as o) {
      <div class="modal-backdrop" (click)="confirming.set(null)">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>Confirmar recepción</h2>
          <p>¿Confirmas que has recibido el <strong>pedido #{{ o.id }}</strong>?</p>
          <p class="muted">Al confirmarlo podrás valorar los productos de tu compra.</p>
          @if (confirmError()) {
            <p class="error">{{ confirmError() }}</p>
          }
          <div class="modal-actions">
            <button class="ghost" (click)="confirming.set(null)">Cancelar</button>
            <button (click)="acceptConfirm(o)" [disabled]="confirmBusy()">Sí, lo he recibido</button>
          </div>
        </div>
      </div>
    }

    @if (reviewing(); as o) {
      <div class="modal-backdrop" (click)="closeReview()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>Valora tu compra</h2>
          <p class="muted">Pedido #{{ o.id }} · comparte tu opinión de cada producto.</p>
          <ul class="review-items">
            @for (i of o.items; track i.productId) {
              <li>
                <span class="item-title">{{ i.title }}</span>
                @if (draft(i.productId).done) {
                  <p class="done">✓ ¡Gracias por tu reseña!</p>
                } @else {
                  <div class="stars-input">
                    @for (n of [1, 2, 3, 4, 5]; track n) {
                      <button
                        type="button"
                        class="star"
                        [class.on]="n <= draft(i.productId).rating"
                        (click)="setRating(i.productId, n)"
                        [attr.aria-label]="n + ' estrellas'"
                      >
                        ★
                      </button>
                    }
                  </div>
                  <textarea
                    placeholder="Cuéntanos qué te ha parecido (opcional)"
                    [value]="draft(i.productId).comment"
                    (input)="setComment(i.productId, $event)"
                  ></textarea>
                  @if (draft(i.productId).error) {
                    <p class="error">{{ draft(i.productId).error }}</p>
                  }
                  <button (click)="publish(i.productId)" [disabled]="draft(i.productId).busy">
                    Publicar reseña
                  </button>
                }
              </li>
            }
          </ul>
          <div class="modal-actions">
            <button class="ghost" (click)="closeReview()">Cerrar</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .orders { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 1rem; }
    .order {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 1rem 1.15rem;
      box-shadow: var(--shadow);
    }
    .order header { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
    .order header span:first-child { color: var(--muted); font-size: 0.9rem; }
    .lines { list-style: none; padding: 0; margin: 0.75rem 0; }
    .lines li { padding: 0.15rem 0; }
    .order footer { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
    .badge {
      font-size: 0.8rem; font-weight: 600; padding: 0.2rem 0.6rem; border-radius: 999px;
      background: var(--border); color: var(--text);
    }
    .badge[data-status='PAID'] { background: #e7eef6; color: #2c5b8a; }
    .badge[data-status='SHIPPED'] { background: #fbf1dd; color: var(--gold-dark); }
    .badge[data-status='DELIVERED'] { background: #e3f1e6; color: #2f7a43; }

    button.ghost {
      background: transparent; color: var(--brown-700);
      border: 1px solid var(--border);
    }
    button.ghost:hover { background: var(--surface); border-color: var(--gold); }

    /* ---- Modal ---- */
    .modal-backdrop {
      position: fixed; inset: 0; z-index: 100;
      background: rgba(0, 0, 0, 0.45);
      display: flex; align-items: center; justify-content: center;
      padding: 1rem;
    }
    .modal {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
      padding: 1.5rem;
      width: 100%; max-width: 460px;
      max-height: 85vh; overflow-y: auto;
    }
    .modal h2 { margin: 0 0 0.75rem; }
    .modal .muted { color: var(--muted); font-size: 0.9rem; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.25rem; }
    .error { color: var(--danger); font-weight: 600; }

    .review-items { list-style: none; padding: 0; margin: 1rem 0 0; display: flex; flex-direction: column; gap: 1rem; }
    .review-items li {
      border: 1px solid var(--border); border-radius: var(--radius);
      padding: 0.85rem 1rem;
    }
    .item-title { display: block; font-weight: 600; margin-bottom: 0.5rem; }
    .review-items textarea { width: 100%; min-height: 3.5rem; margin-bottom: 0.5rem; }
    .review-items button { display: inline-block; }
    .done { color: #2f7a43; font-weight: 600; margin: 0.25rem 0 0; }

    .stars-input { display: flex; gap: 0.15rem; margin-bottom: 0.5rem; }
    .star {
      background: transparent; border: none; padding: 0;
      font-size: 1.6rem; line-height: 1; cursor: pointer;
      color: var(--border);
    }
    .star.on { color: var(--gold); }
    .star:hover { color: var(--gold-dark); }
  `,
})
export class Orders implements OnInit {
  private service = inject(OrderService);
  private catalog = inject(CatalogService);

  orders = signal<Order[]>([]);

  confirming = signal<Order | null>(null);
  confirmBusy = signal(false);
  confirmError = signal('');

  reviewing = signal<Order | null>(null);
  private drafts = signal<Record<number, ReviewDraft>>({});

  ngOnInit() {
    this.reload();
  }

  reload() {
    this.service.myOrders().subscribe((o) => this.orders.set(o));
  }

  label(status: Order['status']) {
    return STATUS_LABELS[status];
  }

  pay(order: Order) {
    this.service.pay(order.id).subscribe(() => this.reload());
  }

  // ── Confirmar recepción ──────────────────────────────────────────────────

  openConfirm(order: Order) {
    this.confirmError.set('');
    this.confirming.set(order);
  }

  acceptConfirm(order: Order) {
    this.confirmBusy.set(true);
    this.service.confirm(order.id).subscribe({
      next: (updated) => {
        this.confirmBusy.set(false);
        this.orders.update((list) => list.map((o) => (o.id === updated.id ? updated : o)));
        this.confirming.set(null);
        this.openReview(updated);
      },
      error: () => {
        this.confirmBusy.set(false);
        this.confirmError.set('No se pudo confirmar la recepción. Inténtalo de nuevo.');
      },
    });
  }

  // ── Valorar compra ───────────────────────────────────────────────────────

  openReview(order: Order) {
    const initial: Record<number, ReviewDraft> = {};
    for (const item of order.items) {
      initial[item.productId] = { rating: 5, comment: '', busy: false, done: false, error: '' };
    }
    this.drafts.set(initial);
    this.reviewing.set(order);
  }

  closeReview() {
    this.reviewing.set(null);
  }

  draft(productId: number): ReviewDraft {
    return this.drafts()[productId] ?? { rating: 5, comment: '', busy: false, done: false, error: '' };
  }

  private patchDraft(productId: number, patch: Partial<ReviewDraft>) {
    this.drafts.update((all) => ({ ...all, [productId]: { ...this.draft(productId), ...patch } }));
  }

  setRating(productId: number, rating: number) {
    this.patchDraft(productId, { rating });
  }

  setComment(productId: number, event: Event) {
    this.patchDraft(productId, { comment: (event.target as HTMLTextAreaElement).value });
  }

  publish(productId: number) {
    const draft = this.draft(productId);
    this.patchDraft(productId, { busy: true, error: '' });
    this.catalog
      .createReview(productId, { rating: draft.rating, comment: draft.comment || null })
      .subscribe({
        next: () => this.patchDraft(productId, { busy: false, done: true }),
        error: (e) => {
          const error =
            e.status === 409
              ? 'Ya has reseñado este producto.'
              : e.status === 403
                ? 'Solo puedes reseñar productos que has recibido.'
                : 'No se pudo publicar la reseña.';
          this.patchDraft(productId, { busy: false, error });
        },
      });
  }
}
