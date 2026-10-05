import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideHeart, LucideImageOff } from '@lucide/angular';
import { Product } from '../../core/catalog/catalog.service';
import { WishlistService } from '../../core/wishlist/wishlist.service';

@Component({
  selector: 'app-favoritos',
  imports: [RouterLink, DecimalPipe, LucideHeart, LucideImageOff],
  template: `
    <h1>Favoritos</h1>

    @if (loading()) {
      <p class="muted">Cargando…</p>
    } @else if (products().length === 0) {
      <div class="empty">
        <svg lucideHeart></svg>
        <p>Todavía no has guardado ningún producto.</p>
        <a routerLink="/">Explorar productos</a>
      </div>
    } @else {
      <ul class="grid">
        @for (p of products(); track p.id) {
          <li>
            <a [routerLink]="['/producto', p.id]">
              @if (p.images.length) {
                <img [src]="p.images[0]" [alt]="p.title" />
              } @else {
                <div class="no-image"><svg lucideImageOff></svg></div>
              }
              <h3>{{ p.title }}</h3>
              <p>{{ p.price | number: '1.2-2' }} €</p>
              <small>{{ p.shopName }} · {{ p.stock > 0 ? 'En stock' : 'Agotado' }}</small>
            </a>
            <button type="button" class="unsave" aria-label="Quitar de favoritos" title="Quitar de favoritos"
                    (click)="remove(p)">
              <svg lucideHeart></svg>
            </button>
          </li>
        }
      </ul>
    }
  `,
  styles: `
    .muted { color: var(--muted); }
    .empty {
      display: flex; flex-direction: column; align-items: center; gap: 0.25rem;
      background: var(--surface); border: 1px dashed var(--border); border-radius: var(--radius);
      padding: 2.5rem 1rem; text-align: center; color: var(--muted);
    }
    .empty svg { width: 32px; height: 32px; color: var(--border); }
    .empty p { margin: 0.25rem 0; }
    .grid li { position: relative; }
    .no-image {
      aspect-ratio: 4 / 3; display: flex; align-items: center; justify-content: center;
      background: #f2e9db; color: var(--muted);
    }
    .no-image svg { width: 36px; height: 36px; }
    .unsave {
      position: absolute; top: 0.6rem; right: 0.6rem; display: inline-flex; padding: 0.45rem;
      border-radius: 50%; border: none; background: rgba(255, 255, 255, 0.92);
      color: #c0392b; box-shadow: var(--shadow);
    }
    .unsave:hover { background: #fff; }
    .unsave svg { width: 18px; height: 18px; fill: currentColor; }
  `,
})
export class Favoritos implements OnInit {
  private wishlist = inject(WishlistService);

  products = signal<Product[]>([]);
  loading = signal(true);

  ngOnInit() {
    this.wishlist.list().subscribe({
      next: (products) => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  remove(product: Product) {
    this.wishlist.toggle(product.id).subscribe(() =>
      this.products.update((list) => list.filter((p) => p.id !== product.id)),
    );
  }
}
