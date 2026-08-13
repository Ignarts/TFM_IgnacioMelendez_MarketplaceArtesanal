import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { Observable, forkJoin, of, switchMap, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Product } from '../catalog/catalog.service';

export type OrderStatus = 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED';

export const STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Pendiente',
  PAID: 'Pagado',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregado',
};

export interface OrderItem {
  productId: number;
  title: string;
  image: string | null;
  price: number;
  quantity: number;
}

export interface Order {
  id: number;
  buyerId: number;
  shopId: number;
  status: OrderStatus;
  total: number;
  createdAt: string;
  items: OrderItem[];
}

/** Slim product snapshot kept in the cart (enough to render and check out). */
export interface CartItem {
  productId: number;
  title: string;
  price: number;
  image: string | null;
  stock: number;
  quantity: number;
}

const CART_KEY = 'cart';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  // Cart lives only in the browser (no Cart entity): the backend just receives the lines at checkout.
  readonly items = signal<CartItem[]>(this.load());
  readonly count = computed(() => this.items().reduce((n, i) => n + i.quantity, 0));
  readonly subtotal = computed(() => this.items().reduce((s, i) => s + i.price * i.quantity, 0));

  constructor() {
    effect(() => localStorage.setItem(CART_KEY, JSON.stringify(this.items())));
  }

  add(product: Product, quantity = 1): void {
    this.items.update((items) => {
      const existing = items.find((i) => i.productId === product.id);
      if (existing) {
        const qty = Math.min(existing.quantity + quantity, product.stock);
        return items.map((i) => (i.productId === product.id ? { ...i, quantity: qty } : i));
      }
      return [
        ...items,
        {
          productId: product.id,
          title: product.title,
          price: product.price,
          image: product.images[0] ?? null,
          stock: product.stock,
          quantity: Math.min(quantity, product.stock),
        },
      ];
    });
  }

  setQuantity(productId: number, quantity: number): void {
    this.items.update((items) =>
      items.map((i) =>
        i.productId === productId ? { ...i, quantity: Math.max(1, Math.min(quantity, i.stock)) } : i,
      ),
    );
  }

  remove(productId: number): void {
    this.items.update((items) => items.filter((i) => i.productId !== productId));
  }

  clear(): void {
    this.items.set([]);
  }

  /**
   * Checkout the current cart; the backend splits it into one order per shop.
   * Immediately pays each resulting order (simulated) so the buyer sees a completed
   * purchase instead of a pending one they'd have to pay separately in "Mis pedidos".
   */
  checkout(): Observable<Order[]> {
    const body = { items: this.items().map((i) => ({ productId: i.productId, quantity: i.quantity })) };
    return this.http.post<Order[]>(`${this.base}/orders`, body).pipe(
      switchMap((orders) => (orders.length ? forkJoin(orders.map((o) => this.pay(o.id))) : of([]))),
      tap(() => this.clear()),
    );
  }

  myOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.base}/orders`);
  }

  pay(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.base}/orders/${id}/pay`, {});
  }

  confirm(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.base}/orders/${id}/confirm`, {});
  }

  shopOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.base}/seller/orders`);
  }

  ship(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.base}/seller/orders/${id}/ship`, {});
  }

  private load(): CartItem[] {
    try {
      return JSON.parse(localStorage.getItem(CART_KEY) ?? '[]');
    } catch {
      return [];
    }
  }
}
