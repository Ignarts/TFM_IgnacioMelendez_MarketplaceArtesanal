import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideCircleCheck } from '@lucide/angular';
import { AuthService } from '../../core/auth/auth.service';
import { Order, OrderService } from '../../core/order/order.service';

const FREE_SHIPPING_FROM = 50;

@Component({
  selector: 'app-cart',
  imports: [RouterLink, DecimalPipe, LucideCircleCheck],
  template: `
    @if (completedOrders(); as orders) {
      <div class="success">
        <h1><svg lucideCircleCheck></svg> ¡Compra realizada!</h1>
        <p class="muted">Te hemos enviado un correo con el resumen del pedido.</p>

        <ul class="orders">
          @for (o of orders; track o.id) {
            <li class="order">
              <header>Pedido #{{ o.id }} <span class="paid-badge">Pagado ✓</span></header>
              <ul class="lines">
                @for (i of o.items; track i.productId) {
                  <li>
                    {{ i.title }} × {{ i.quantity }} — {{ i.price * i.quantity | number: '1.2-2' }} €
                  </li>
                }
              </ul>
              <footer>Subtotal: <strong>{{ o.total | number: '1.2-2' }} €</strong></footer>
            </li>
          }
        </ul>

        <p class="grand-total">Total pagado: <strong>{{ grandTotal(orders) | number: '1.2-2' }} €</strong></p>

        <div class="actions">
          <a routerLink="/pedidos" class="link-button">Ver mis pedidos</a>
          <a routerLink="/">Seguir comprando</a>
        </div>
      </div>
    } @else if (cart.items().length === 0) {
      <h1>Tu carrito</h1>
      <p>Tu carrito está vacío. <a routerLink="/">Explora productos</a>.</p>
    } @else {
      <h1>Tu carrito</h1>
      <ul class="cart-list">
        @for (i of cart.items(); track i.productId) {
          <li class="cart-row">
            @if (i.image) { <img [src]="i.image" [alt]="i.title" /> } @else { <div class="noimg"></div> }
            <div class="info">
              <a [routerLink]="['/producto', i.productId]">{{ i.title }}</a>
              <small>{{ i.price | number: '1.2-2' }} € · stock {{ i.stock }}</small>
            </div>
            <input
              type="number"
              min="1"
              [max]="i.stock"
              [value]="i.quantity"
              (change)="updateQty(i.productId, $event)"
            />
            <span class="line-total">{{ i.price * i.quantity | number: '1.2-2' }} €</span>
            <button type="button" (click)="cart.remove(i.productId)">Quitar</button>
          </li>
        }
      </ul>

      <div class="summary">
        <p>Subtotal: <strong>{{ cart.subtotal() | number: '1.2-2' }} €</strong></p>
        @if (remainingForFreeShipping() > 0) {
          <p class="shipping">
            Te faltan {{ remainingForFreeShipping() | number: '1.2-2' }} € para el envío gratis.
          </p>
        } @else {
          <p class="shipping free">Envío gratis 🎉</p>
        }
        @if (error()) { <p class="error">{{ error() }}</p> }
        <button (click)="checkout()" [disabled]="loading()">
          {{ loading() ? 'Procesando…' : 'Finalizar compra' }}
        </button>
      </div>
    }
  `,
  styles: `
    .success h1 { display: flex; align-items: center; gap: 0.5rem; }
    .success h1 svg { width: 28px; height: 28px; color: #2f7a43; }
    .muted { color: var(--muted); }
    .orders { list-style: none; padding: 0; margin: 1.5rem 0; display: flex; flex-direction: column; gap: 1rem; }
    .order {
      background: var(--surface); border: 1px solid var(--border);
      border-radius: var(--radius); padding: 1rem 1.15rem; box-shadow: var(--shadow);
    }
    .order header { display: flex; align-items: center; gap: 0.6rem; color: var(--muted); font-size: 0.9rem; }
    .paid-badge {
      font-size: 0.78rem; font-weight: 600; color: #2f7a43;
      background: #e3f1e6; border-radius: 999px; padding: 0.1rem 0.6rem;
    }
    .lines { list-style: none; padding: 0; margin: 0.75rem 0; }
    .lines li { padding: 0.15rem 0; }
    .order footer { text-align: right; }
    .grand-total { font-size: 1.2rem; margin: 0 0 1.5rem; }
    .actions { display: flex; align-items: center; gap: 1.25rem; }
    .link-button {
      display: inline-block; padding: 0.55rem 1.1rem; border-radius: 999px;
      background: var(--eggplant-700); color: #fff;
    }
    .link-button:hover { background: var(--eggplant-600); color: #fff; }

    .cart-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.75rem; }
    .cart-row {
      display: grid;
      grid-template-columns: 64px 1fr 80px 90px auto;
      align-items: center;
      gap: 0.85rem;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 0.6rem 0.85rem;
    }
    .cart-row img, .cart-row .noimg {
      width: 64px; height: 64px; object-fit: cover; border-radius: 8px; background: var(--border);
    }
    .info { display: flex; flex-direction: column; gap: 0.15rem; }
    .line-total { font-weight: 600; color: var(--gold-dark); text-align: right; }
    .summary { margin-top: 1.5rem; }
    .shipping { color: var(--muted); margin: 0.25rem 0 0.75rem; }
    .shipping.free { color: var(--gold-dark); font-weight: 600; }
    @media (max-width: 600px) {
      .cart-row { grid-template-columns: 48px 1fr auto; }
      .cart-row .line-total, .cart-row input { grid-column: 2 / 4; justify-self: start; }
    }
  `,
})
export class Cart {
  cart = inject(OrderService);
  private auth = inject(AuthService);
  private router = inject(Router);

  error = signal('');
  loading = signal(false);
  completedOrders = signal<Order[] | null>(null);

  remainingForFreeShipping = computed(() => Math.max(0, FREE_SHIPPING_FROM - this.cart.subtotal()));

  updateQty(productId: number, event: Event) {
    const value = Number((event.target as HTMLInputElement).value);
    this.cart.setQuantity(productId, value);
  }

  grandTotal(orders: Order[]) {
    return orders.reduce((sum, o) => sum + o.total, 0);
  }

  checkout() {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }
    this.error.set('');
    this.loading.set(true);
    this.cart.checkout().subscribe({
      next: (orders) => {
        this.loading.set(false);
        this.completedOrders.set(orders);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(e.status === 409 ? 'Algún producto se ha quedado sin stock' : 'No se pudo completar la compra');
      },
    });
  }
}
