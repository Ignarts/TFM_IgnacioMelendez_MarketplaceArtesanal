import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Order, OrderService, STATUS_LABELS } from '../../core/order/order.service';

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
              } @else if (o.status === 'SHIPPED') {
                <button (click)="confirm(o)">Confirmar recepción</button>
              }
            </footer>
          </li>
        }
      </ul>
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
  `,
})
export class Orders implements OnInit {
  private service = inject(OrderService);
  orders = signal<Order[]>([]);

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

  confirm(order: Order) {
    this.service.confirm(order.id).subscribe(() => this.reload());
  }
}
