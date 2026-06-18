import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { OrderService } from '../../core/order/order.service';

const FREE_SHIPPING_FROM = 50;

@Component({
  selector: 'app-cart',
  imports: [RouterLink, DecimalPipe],
  template: `
    <h1>Tu carrito</h1>

    @if (cart.items().length === 0) {
      <p>Tu carrito está vacío. <a routerLink="/">Explora productos</a>.</p>
    } @else {
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

  remainingForFreeShipping = computed(() => Math.max(0, FREE_SHIPPING_FROM - this.cart.subtotal()));

  updateQty(productId: number, event: Event) {
    const value = Number((event.target as HTMLInputElement).value);
    this.cart.setQuantity(productId, value);
  }

  checkout() {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }
    this.error.set('');
    this.loading.set(true);
    this.cart.checkout().subscribe({
      next: () => this.router.navigate(['/pedidos']),
      error: (e) => {
        this.loading.set(false);
        this.error.set(e.status === 409 ? 'Algún producto se ha quedado sin stock' : 'No se pudo completar la compra');
      },
    });
  }
}
