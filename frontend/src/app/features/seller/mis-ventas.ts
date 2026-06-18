import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Order, OrderService, STATUS_LABELS } from '../../core/order/order.service';

@Component({
  selector: 'app-mis-ventas',
  imports: [RouterLink, DecimalPipe, DatePipe],
  template: `
    <h1>Mis ventas</h1>

    <div class="kpis">
      <div class="kpi"><span>{{ orders().length }}</span><small>Pedidos</small></div>
      <div class="kpi"><span>{{ pendingToShip() }}</span><small>Por enviar</small></div>
      <div class="kpi"><span>{{ revenue() | number: '1.2-2' }} €</span><small>Ingresos</small></div>
    </div>

    @if (orders().length === 0) {
      <p>Tu tienda todavía no ha recibido pedidos.</p>
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
              @if (o.status === 'PAID') {
                <button (click)="ship(o)">Marcar como enviado</button>
              }
            </footer>
          </li>
        }
      </ul>
    }
  `,
  styles: `
    .kpis { display: flex; gap: 1rem; flex-wrap: wrap; margin-bottom: 1.5rem; }
    .kpi {
      flex: 1 1 120px; background: var(--surface); border: 1px solid var(--border);
      border-radius: var(--radius); padding: 1rem; text-align: center; box-shadow: var(--shadow);
    }
    .kpi span { display: block; font-size: 1.5rem; font-weight: 700; color: var(--eggplant-700); }
    .kpi small { color: var(--muted); }
    .orders { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 1rem; }
    .order {
      background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius);
      padding: 1rem 1.15rem; box-shadow: var(--shadow);
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
export class MisVentas implements OnInit {
  private service = inject(OrderService);
  orders = signal<Order[]>([]);

  pendingToShip = computed(() => this.orders().filter((o) => o.status === 'PAID').length);
  // Revenue counts orders that are at least paid.
  revenue = computed(() =>
    this.orders().filter((o) => o.status !== 'PENDING').reduce((s, o) => s + o.total, 0),
  );

  ngOnInit() {
    this.reload();
  }

  reload() {
    this.service.shopOrders().subscribe((o) => this.orders.set(o));
  }

  label(status: Order['status']) {
    return STATUS_LABELS[status];
  }

  ship(order: Order) {
    this.service.ship(order.id).subscribe(() => this.reload());
  }
}
