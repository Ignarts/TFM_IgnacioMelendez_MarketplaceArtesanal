import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CatalogService, Product, Review } from '../../core/catalog/catalog.service';
import { OrderService } from '../../core/order/order.service';

@Component({
  selector: 'app-producto',
  imports: [RouterLink, DecimalPipe, DatePipe, ReactiveFormsModule],
  template: `
    @if (product(); as p) {
      <p><a routerLink="/">← Volver al explorador</a></p>
      <h1>{{ p.title }}</h1>
      <p class="price">{{ p.price | number: '1.2-2' }} €</p>

      @if (averageRating(); as avg) {
        <p class="rating-summary">{{ stars(avg) }} {{ avg | number: '1.1-1' }} ({{ reviews().length }})</p>
      }

      @if (p.images.length) {
        <div class="images">
          @for (img of p.images; track img) { <img [src]="img" [alt]="p.title" /> }
        </div>
      }
      <p>{{ p.description }}</p>
      <ul>
        <li><strong>Tienda:</strong> {{ p.shopName }}</li>
        <li><strong>Categoría:</strong> {{ p.categoryName }}</li>
        <li><strong>Stock:</strong> {{ p.stock }}</li>
      </ul>

      <div class="buy-box">
        @if (p.stock > 0) {
          <input type="number" min="1" [max]="p.stock" [value]="quantity()" (change)="setQty($event)" />
          <button (click)="addToCart(p)">Añadir al carrito</button>
          @if (added()) { <span class="added">Añadido ✓</span> }
        } @else {
          <p>Sin stock.</p>
        }
      </div>

      <h2>Reseñas</h2>
      @if (reviews().length === 0) {
        <p>Todavía no hay reseñas de este producto.</p>
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
          <label>Puntuación
            <select formControlName="rating">
              @for (n of [5, 4, 3, 2, 1]; track n) { <option [value]="n">{{ stars(n) }}</option> }
            </select>
          </label>
          <label>Comentario <textarea formControlName="comment"></textarea></label>
          @if (reviewError()) { <p class="error">{{ reviewError() }}</p> }
          <button type="submit">Publicar reseña</button>
        </form>
      } @else {
        <p><a routerLink="/login">Inicia sesión</a> para dejar una reseña (solo compradores verificados).</p>
      }
    } @else if (notFound()) {
      <p>Producto no encontrado.</p>
    } @else {
      <p>Cargando…</p>
    }
  `,
  styles: `
    .rating-summary { color: var(--gold-dark); font-weight: 600; margin-top: -0.5rem; }
    .buy-box { display: flex; align-items: center; gap: 0.75rem; margin: 1.5rem 0; }
    .buy-box input { width: 80px; }
    .added { color: #2f7a43; font-weight: 600; }
    .reviews { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.85rem; }
    .reviews li {
      background: var(--surface); border: 1px solid var(--border);
      border-radius: var(--radius); padding: 0.85rem 1rem;
    }
    .review-head { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
    .verified { font-size: 0.78rem; color: #2f7a43; }
    .stars { color: var(--gold); letter-spacing: 2px; }
  `,
})
export class Producto implements OnInit {
  private catalog = inject(CatalogService);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private cart = inject(OrderService);
  auth = inject(AuthService);

  product = signal<Product | null>(null);
  reviews = signal<Review[]>([]);
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
      next: (p) => this.product.set(p),
      error: () => this.notFound.set(true),
    });
    this.loadReviews(id);
  }

  loadReviews(id: number) {
    this.catalog.reviews(id).subscribe((r) => this.reviews.set(r));
  }

  stars(rating: number) {
    const full = Math.round(rating);
    return '★'.repeat(full) + '☆'.repeat(5 - full);
  }

  setQty(event: Event) {
    this.quantity.set(Number((event.target as HTMLInputElement).value));
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
