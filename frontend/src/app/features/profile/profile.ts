import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { Order, OrderService, STATUS_LABELS } from '../../core/order/order.service';

@Component({
  selector: 'app-profile',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile implements OnInit {
  auth = inject(AuthService);
  private orders = inject(OrderService);
  private router = inject(Router);

  readonly statusLabels = STATUS_LABELS;
  readonly recentPurchases = signal<Order[]>([]);
  readonly recentSales = signal<Order[]>([]);
  readonly purchasesLoaded = signal(false);
  readonly salesLoaded = signal(false);
  readonly isSeller = computed(() => this.auth.user()?.roles.includes('SELLER') ?? false);

  ngOnInit() {
    this.auth.me().subscribe(() => this.loadActivity());
  }

  private loadActivity() {
    this.orders.myOrders().subscribe((orders) => {
      this.recentPurchases.set(this.latest(orders));
      this.purchasesLoaded.set(true);
    });

    if (this.isSeller()) {
      this.orders.shopOrders().subscribe((orders) => {
        this.recentSales.set(this.latest(orders));
        this.salesLoaded.set(true);
      });
    }
  }

  /** Most recent orders first, capped to the three shown in the profile summary. */
  private latest(orders: Order[]): Order[] {
    return [...orders]
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      .slice(0, 3);
  }

  itemCount(order: Order): number {
    return order.items.reduce((n, i) => n + i.quantity, 0);
  }

  initials(name: string): string {
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  }

  roleLabel(role: string): string {
    const labels: Record<string, string> = {
      ADMIN: 'Administrador',
      SELLER: 'Vendedor',
      BUYER: 'Comprador',
    };
    return labels[role] ?? role;
  }

  accountType(roles: string[]): string {
    if (roles.includes('ADMIN')) return 'Administrador';
    if (roles.includes('SELLER')) return 'Vendedor';
    return 'Comprador';
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
