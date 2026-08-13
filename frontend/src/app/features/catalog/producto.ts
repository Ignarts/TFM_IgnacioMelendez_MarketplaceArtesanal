import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  LucideArrowLeft,
  LucideBadgeCheck,
  LucideCheck,
  LucideImageOff,
  LucideShoppingCart,
  LucideStore,
} from '@lucide/angular';
import { AuthService } from '../../core/auth/auth.service';
import { CatalogService, Product, Review } from '../../core/catalog/catalog.service';
import { OrderService } from '../../core/order/order.service';
import { Reputation, ReputationService, BADGE_LABELS } from '../../core/reputation/reputation.service';

@Component({
  selector: 'app-producto',
  imports: [
    RouterLink,
    DecimalPipe,
    DatePipe,
    ReactiveFormsModule,
    LucideArrowLeft,
    LucideBadgeCheck,
    LucideCheck,
    LucideImageOff,
    LucideShoppingCart,
    LucideStore,
  ],
  template: `
    @if (product(); as p) {
      <a routerLink="/" class="back-link"><svg lucideArrowLeft></svg> Volver al explorador</a>

      <div class="product-layout">
        <div class="gallery">
          @if (p.images.length) {
            <div class="gallery-main">
              <img [src]="activeImage()" [alt]="p.title" />
            </div>
            @if (p.images.length > 1) {
              <div class="gallery-thumbs">
                @for (img of p.images; track img) {
                  <button
                    type="button"
                    class="thumb"
                    [class.active]="img === activeImage()"
                    (click)="activeImage.set(img)"
                  >
                    <img [src]="img" [alt]="p.title" />
                  </button>
                }
              </div>
            }
          } @else {
            <div class="gallery-main placeholder"><svg lucideImageOff></svg></div>
          }
        </div>

        <div class="details">
          <h1>{{ p.title }}</h1>

          @if (averageRating(); as avg) {
            <p class="rating-summary">
              <span class="stars">{{ stars(avg) }}</span>
              <strong>{{ avg | number: '1.1-1' }}</strong>
              <span class="muted">({{ reviews().length }} reseña{{ reviews().length === 1 ? '' : 's' }})</span>
            </p>
          } @else {
            <p class="rating-summary muted">Todavía sin reseñas</p>
          }

          <p class="price">{{ p.price | number: '1.2-2' }} €</p>

          @if (p.description) {
            <p class="description">{{ p.description }}</p>
          }

          <div class="shop-card">
            <span class="shop-name"><svg lucideStore></svg> {{ p.shopName }}</span>
            @if (reputation(); as rep) {
              <span class="rep-score" title="Puntuación de reputación">{{ rep.score }}/100</span>
              @for (badge of rep.badges; track badge) {
                <span class="badge"><svg lucideBadgeCheck></svg> {{ badgeLabel(badge) }}</span>
              }
            }
          </div>

          <ul class="meta-list">
            <li><strong>Categoría:</strong> {{ p.categoryName }}</li>
            <li><strong>Stock:</strong> {{ p.stock }} unidades</li>
          </ul>

          <div class="buy-box">
            @if (p.stock > 0) {
              <input type="number" min="1" [max]="p.stock" [value]="quantity()" (change)="setQty($event)" />
              <button (click)="addToCart(p)"><svg lucideShoppingCart></svg> Añadir al carrito</button>
              @if (added()) { <span class="added"><svg lucideCheck></svg> Añadido</span> }
            } @else {
              <p class="out-of-stock">Sin stock disponible.</p>
            }
          </div>
        </div>
      </div>

      <section class="reviews-section">
        <h2>Reseñas</h2>
        @if (reviews().length === 0) {
          <p class="muted">Todavía no hay reseñas de este producto.</p>
        } @else {
          <ul class="reviews">
            @for (r of reviews(); track r.id) {
              <li>
                <div class="review-head">
                  <strong>{{ r.buyerName }}</strong>
                  <span class="verified">Compra verificada ✓</span>
                </div>
                <span class="stars">{{ stars(r.rating) }}</span>
                <small>{{ r.createdAt | date: 'dd/MM/yyyy' }}</small>
                @if (r.comment) { <p>{{ r.comment }}</p> }
              </li>
            }
          </ul>
        }

        @if (auth.isLoggedIn()) {
          <h3>Deja tu valoración</h3>
          <form [formGroup]="form" (ngSubmit)="submitReview(p.id)">
            <div class="rating-field">
              <span class="field-label">Puntuación</span>
              <div class="stars-input">
                @for (n of [1, 2, 3, 4, 5]; track n) {
                  <button
                    type="button"
                    class="star"
                    [class.on]="n <= form.controls.rating.value"
                    (click)="setRating(n)"
                    [attr.aria-label]="n + ' estrellas'"
                  >
                    ★
                  </button>
                }
              </div>
            </div>
            <label>Comentario <textarea formControlName="comment"></textarea></label>
            @if (reviewError()) { <p class="error">{{ reviewError() }}</p> }
            <button type="submit">Publicar reseña</button>
          </form>
        } @else {
          <p><a routerLink="/login">Inicia sesión</a> para dejar una reseña (solo compradores verificados).</p>
        }
      </section>
    } @else if (notFound()) {
      <p>Producto no encontrado.</p>
    } @else {
      <p>Cargando…</p>
    }
  `,
  styles: `
    .back-link {
      display: inline-flex; align-items: center; gap: 0.4rem;
      font-size: 0.9rem; margin-bottom: 1.25rem;
    }
    .back-link svg { width: 16px; height: 16px; }

    .muted { color: var(--muted); }

    /* ---- Layout ---- */
    .product-layout {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
      gap: 2.5rem;
      align-items: start;
    }
    @media (max-width: 720px) {
      .product-layout { grid-template-columns: 1fr; gap: 1.5rem; }
    }

    /* ---- Gallery ---- */
    .gallery { position: sticky; top: 1.5rem; display: flex; flex-direction: column; gap: 0.75rem; }
    .gallery-main {
      aspect-ratio: 4 / 3;
      border-radius: var(--radius);
      border: 1px solid var(--border);
      overflow: hidden;
      background: var(--surface);
      box-shadow: var(--shadow);
    }
    .gallery-main img { width: 100%; height: 100%; object-fit: cover; display: block; }
    .gallery-main.placeholder {
      display: flex; align-items: center; justify-content: center;
      color: var(--muted); background: #f2e9db;
    }
    .gallery-main.placeholder svg { width: 48px; height: 48px; }

    .gallery-thumbs { display: flex; flex-wrap: wrap; gap: 0.6rem; }
    .thumb {
      padding: 0; width: 64px; height: 64px; border-radius: 8px;
      border: 2px solid var(--border); background: var(--surface); overflow: hidden;
    }
    .thumb:hover { background: var(--surface); border-color: var(--gold); }
    .thumb.active { border-color: var(--gold-dark); }
    .thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }

    /* ---- Details ---- */
    h1 { margin-bottom: 0.4rem; }
    .rating-summary { display: flex; align-items: center; gap: 0.4rem; margin: 0 0 0.5rem; }
    .stars { color: var(--gold); letter-spacing: 2px; }
    .price { font-size: 1.75rem; font-weight: 700; color: var(--gold-dark); margin: 0 0 1rem; }
    .description { margin: 0 0 1.25rem; }

    .shop-card {
      display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem;
      background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius);
      padding: 0.75rem 1rem; margin-bottom: 1.25rem;
    }
    .shop-name { display: inline-flex; align-items: center; gap: 0.4rem; font-weight: 600; color: var(--brown-700); }
    .shop-name svg { width: 18px; height: 18px; }
    .rep-score {
      font-size: 0.8rem; background: var(--brown-700); color: #fff;
      border-radius: 999px; padding: 2px 9px; font-weight: 600;
    }
    .badge {
      display: inline-flex; align-items: center; gap: 0.3rem;
      font-size: 0.75rem; background: #f2e9db; color: var(--brown-700);
      border: 1px solid var(--border); border-radius: 999px; padding: 2px 9px;
    }
    .badge svg { width: 13px; height: 13px; }

    .meta-list { list-style: none; padding: 0; margin: 0 0 1.5rem; display: flex; flex-direction: column; gap: 0.3rem; color: var(--muted); }
    .meta-list strong { color: var(--text); }

    .buy-box { display: flex; align-items: center; gap: 0.75rem; }
    .buy-box input { width: 70px; }
    .buy-box button { display: inline-flex; align-items: center; gap: 0.45rem; }
    .buy-box button svg { width: 18px; height: 18px; }
    .out-of-stock { color: var(--danger); font-weight: 600; }
    .added { display: inline-flex; align-items: center; gap: 0.3rem; color: #2f7a43; font-weight: 600; }
    .added svg { width: 16px; height: 16px; }

    /* ---- Reviews ---- */
    .reviews-section { margin-top: 3rem; }
    .reviews { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.85rem; }
    .reviews li {
      background: var(--surface); border: 1px solid var(--border);
      border-radius: var(--radius); padding: 0.85rem 1rem;
    }
    .review-head { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
    .verified { font-size: 0.78rem; color: #2f7a43; }

    .rating-field { display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 0.5rem; }
    .field-label { font-weight: 600; }
    .stars-input { display: flex; gap: 0.15rem; }
    .star {
      background: transparent; border: none; padding: 0;
      font-size: 1.6rem; line-height: 1; cursor: pointer;
      color: var(--border);
    }
    .star.on { color: var(--gold); }
    .star:hover { color: var(--gold-dark); }
  `,
})
export class Producto implements OnInit {
  private catalog = inject(CatalogService);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private cart = inject(OrderService);
  private reputationService = inject(ReputationService);
  auth = inject(AuthService);

  product = signal<Product | null>(null);
  activeImage = signal('');
  reviews = signal<Review[]>([]);
  reputation = signal<Reputation | null>(null);
  notFound = signal(false);
  quantity = signal(1);
  added = signal(false);
  reviewError = signal('');

  averageRating = computed(() => {
    const list = this.reviews();
    return list.length ? list.reduce((s, r) => s + r.rating, 0) / list.length : 0;
  });

  form = this.fb.nonNullable.group({
    rating: [5, [Validators.required]],
    comment: [''],
  });

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.catalog.product(id).subscribe({
      next: (p) => {
        this.product.set(p);
        this.activeImage.set(p.images[0] ?? '');
        this.reputationService.getReputation(p.shopId).subscribe({
          next: (rep) => this.reputation.set(rep),
          error: () => {},
        });
      },
      error: () => this.notFound.set(true),
    });
    this.loadReviews(id);
  }

  loadReviews(id: number) {
    this.catalog.reviews(id).subscribe((r) => this.reviews.set(r));
  }

  badgeLabel(badge: string): string {
    return BADGE_LABELS[badge as keyof typeof BADGE_LABELS] ?? badge;
  }

  stars(rating: number) {
    const full = Math.round(rating);
    return '★'.repeat(full) + '☆'.repeat(5 - full);
  }

  setQty(event: Event) {
    this.quantity.set(Number((event.target as HTMLInputElement).value));
  }

  setRating(rating: number) {
    this.form.controls.rating.setValue(rating);
  }

  addToCart(product: Product) {
    this.cart.add(product, this.quantity());
    this.added.set(true);
    setTimeout(() => this.added.set(false), 2000);
  }

  submitReview(productId: number) {
    if (this.form.invalid) return;
    this.reviewError.set('');
    const v = this.form.getRawValue();
    this.catalog.createReview(productId, { rating: Number(v.rating), comment: v.comment || null }).subscribe({
      next: () => {
        this.form.reset({ rating: 5, comment: '' });
        this.loadReviews(productId);
      },
      error: (e) => {
        if (e.status === 403) this.reviewError.set('Solo puedes reseñar productos que has comprado y recibido');
        else if (e.status === 409) this.reviewError.set('Ya has reseñado este producto');
        else this.reviewError.set('No se pudo publicar la reseña');
      },
    });
  }
}
