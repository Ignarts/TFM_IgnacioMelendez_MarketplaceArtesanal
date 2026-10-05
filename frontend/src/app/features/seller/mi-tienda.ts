import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  LucideBadgeCheck,
  LucideImageOff,
  LucidePencil,
  LucidePlus,
  LucideReply,
  LucideTrash2,
} from '@lucide/angular';
import { AuthService, SellerStats } from '../../core/auth/auth.service';
import { CatalogService, Category, Product, Review, Shop } from '../../core/catalog/catalog.service';
import { BADGE_LABELS, Badge, Reputation, ReputationService } from '../../core/reputation/reputation.service';

@Component({
  selector: 'app-mi-tienda',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    DecimalPipe,
    DatePipe,
    LucideBadgeCheck,
    LucideImageOff,
    LucidePencil,
    LucidePlus,
    LucideReply,
    LucideTrash2,
  ],
  template: `
    <!-- ── Shop header ─────────────────────────────────────────────── -->
    @if (shop(); as s) {
      <section class="card shop-header">
        @if (!editingShop()) {
          <div class="shop-title">
            <div>
              <h1>{{ s.name }}</h1>
              @if (s.verified) {
                <span class="pill ok"><svg lucideBadgeCheck></svg> Tienda verificada</span>
              } @else {
                <span class="pill warn">Pendiente de verificación</span>
              }
            </div>
            <button type="button" class="with-icon" (click)="startEditShop(s)">
              <svg lucidePencil></svg> Editar tienda
            </button>
          </div>
          @if (s.description) {
            <p class="shop-desc">{{ s.description }}</p>
          } @else {
            <p class="shop-desc muted">Añade una descripción para que los compradores conozcan tu taller.</p>
          }
        } @else {
          <h2 class="flush">Editar tienda</h2>
          <form [formGroup]="shopForm" (ngSubmit)="saveShop()" class="wide-form">
            <label>Nombre de la tienda <input formControlName="name" maxlength="120" /></label>
            <label>Descripción
              <textarea formControlName="description" rows="4" maxlength="2000"
                        placeholder="Cuenta qué haces, con qué materiales y cómo trabajas."></textarea>
            </label>
            @if (shopError()) { <p class="error">{{ shopError() }}</p> }
            <div class="form-actions">
              <button type="submit" [disabled]="shopForm.invalid">Guardar cambios</button>
              <button type="button" (click)="editingShop.set(false)">Cancelar</button>
            </div>
          </form>
        }
      </section>
    }

    <!-- ── KPIs ────────────────────────────────────────────────────── -->
    <div class="kpis">
      <div class="kpi reputation">
        <span>{{ reputation()?.score ?? '—' }}<small>/100</small></span>
        <small>Reputación</small>
      </div>
      <div class="kpi">
        <span>
          @if (stats()?.reviewsReceived) { {{ stats()!.avgRating | number: '1.1-1' }} <em>★</em> } @else { — }
        </span>
        <small>Valoración media</small>
      </div>
      <div class="kpi"><span>{{ stats()?.reviewsReceived ?? 0 }}</span><small>Reseñas</small></div>
      <div class="kpi"><span>{{ stats()?.productsSold ?? 0 }}</span><small>Unidades vendidas</small></div>
      <div class="kpi"><span>{{ stats()?.productsOnSale ?? 0 }}</span><small>Productos en venta</small></div>
    </div>
    @if (reputation(); as rep) {
      @if (rep.badges.length) {
        <div class="badges">
          @for (badge of rep.badges; track badge) {
            <span class="badge"><svg lucideBadgeCheck></svg> {{ badgeLabel(badge) }}</span>
          }
        </div>
      }
    } @else if (reputationPending()) {
      <p class="hint">Tu reputación todavía no se ha calculado: se actualiza automáticamente cada hora.</p>
    }

    <!-- ── Products ────────────────────────────────────────────────── -->
    <div class="section-head">
      <h2>Mis productos</h2>
      @if (!showForm()) {
        <button class="with-icon" (click)="startCreate()"><svg lucidePlus></svg> Nuevo producto</button>
      }
    </div>

    @if (showForm()) {
      <section class="card" id="product-form">
        <h3>{{ editingId() ? 'Editar producto' : 'Nuevo producto' }}</h3>
        <form [formGroup]="form" (ngSubmit)="submit()" class="wide-form product-form">
          <label class="full">Título <input formControlName="title" /></label>
          <label class="full">Descripción <textarea formControlName="description" rows="3"></textarea></label>
          <label>Precio (€) <input type="number" formControlName="price" min="0" step="0.01" /></label>
          <label>Stock <input type="number" formControlName="stock" min="0" /></label>
          <label>Categoría
            <select formControlName="categoryId">
              <option value="">Selecciona…</option>
              @for (c of categories(); track c.id) { <option [value]="c.id">{{ c.name }}</option> }
            </select>
          </label>
          <label>Imágenes <input formControlName="images" placeholder="URLs separadas por coma" /></label>
          @if (error()) { <p class="error full">{{ error() }}</p> }
          <div class="form-actions full">
            <button type="submit" [disabled]="form.invalid">{{ editingId() ? 'Guardar cambios' : 'Publicar producto' }}</button>
            <button type="button" (click)="cancelEdit()">Cancelar</button>
          </div>
        </form>
      </section>
    }

    @if (listError()) { <p class="error spaced">{{ listError() }}</p> }

    @if (products().length === 0) {
      <p class="empty">Todavía no has publicado productos.</p>
    } @else {
      <ul class="product-list">
        @for (p of products(); track p.id) {
          <li class="product-row" [class.withdrawn]="p.hidden">
            <div class="thumb">
              @if (p.images.length) { <img [src]="p.images[0]" [alt]="p.title" /> }
              @else { <svg lucideImageOff></svg> }
            </div>
            <div class="info">
              @if (p.hidden) { <strong>{{ p.title }}</strong> }
              @else { <a [routerLink]="['/producto', p.id]"><strong>{{ p.title }}</strong></a> }
              <small>{{ p.categoryName }} · Stock: {{ p.stock }}</small>
              @if (p.hidden) {
                <span class="pill danger" title="Un administrador lo ha retirado del catálogo público">
                  Retirado por moderación
                </span>
              } @else if (p.stock === 0) {
                <span class="pill warn">Sin stock</span>
              }
            </div>
            <strong class="price">{{ p.price | number: '1.2-2' }} €</strong>
            <div class="row-actions">
              <button type="button" (click)="edit(p)" aria-label="Editar" title="Editar"><svg lucidePencil></svg></button>
              <button type="button" class="danger" (click)="remove(p)" aria-label="Eliminar" title="Eliminar">
                <svg lucideTrash2></svg>
              </button>
            </div>
          </li>
        }
      </ul>
    }

    <!-- ── Reviews ─────────────────────────────────────────────────── -->
    <div class="section-head">
      <h2>Reseñas recibidas</h2>
      @if (unanswered()) { <span class="pill warn">{{ unanswered() }} sin responder</span> }
    </div>

    @if (reviews().length === 0) {
      <p class="empty">Tus productos todavía no tienen reseñas.</p>
    } @else {
      <ul class="reviews">
        @for (r of reviews(); track r.id) {
          <li class="card review">
            <header>
              <span class="stars">{{ stars(r.rating) }}</span>
              <strong>{{ r.buyerName }}</strong>
              <span class="muted">en <a [routerLink]="['/producto', r.productId]">{{ r.productTitle }}</a></span>
              <small class="date">{{ r.createdAt | date: 'dd/MM/yyyy' }}</small>
            </header>
            @if (r.comment) { <p class="comment">{{ r.comment }}</p> }

            @if (replyingId() === r.id) {
              <form [formGroup]="replyForm" (ngSubmit)="sendReply(r)" class="wide-form reply-form">
                <textarea formControlName="reply" rows="3" maxlength="1000"
                          placeholder="Tu respuesta será pública, debajo de la reseña."></textarea>
                @if (replyError()) { <p class="error">{{ replyError() }}</p> }
                <div class="form-actions">
                  <button type="submit" [disabled]="replyForm.invalid">Publicar respuesta</button>
                  <button type="button" (click)="replyingId.set(null)">Cancelar</button>
                </div>
              </form>
            } @else if (r.sellerReply) {
              <div class="seller-reply">
                <small>Tu respuesta · {{ r.sellerReplyAt | date: 'dd/MM/yyyy' }}</small>
                <p>{{ r.sellerReply }}</p>
                <button type="button" class="link" (click)="startReply(r)">Editar respuesta</button>
              </div>
            } @else {
              <button type="button" class="with-icon" (click)="startReply(r)"><svg lucideReply></svg> Responder</button>
            }
          </li>
        }
      </ul>
    }
  `,
  styles: `
    .muted { color: var(--muted); }
    .card {
      background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius);
      padding: 1.15rem 1.25rem; box-shadow: var(--shadow);
    }
    .with-icon { display: inline-flex; align-items: center; gap: 0.4rem; }
    .with-icon svg { width: 16px; height: 16px; }
    .pill { display: inline-flex; align-items: center; gap: 0.3rem; }
    .pill svg { width: 13px; height: 13px; }

    /* ---- Header ---- */
    .shop-header { margin-bottom: 1.25rem; }
    .shop-title { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; flex-wrap: wrap; }
    .shop-title h1 { margin: 0 0 0.4rem; }
    .shop-desc { margin: 0.85rem 0 0; white-space: pre-line; }
    .flush { margin-top: 0; }
    .wide-form { max-width: none; }
    .form-actions { display: flex; gap: 0.6rem; flex-wrap: wrap; }

    /* ---- KPIs ---- */
    .kpis { display: flex; gap: 1rem; flex-wrap: wrap; margin-bottom: 0.75rem; }
    .kpi {
      flex: 1 1 130px; background: var(--surface); border: 1px solid var(--border);
      border-radius: var(--radius); padding: 1rem; text-align: center; box-shadow: var(--shadow);
    }
    .kpi span { display: block; font-size: 1.5rem; font-weight: 700; color: var(--brown-700); }
    .kpi span small { font-size: 0.85rem; font-weight: 500; }
    .kpi span em { font-style: normal; color: var(--gold); }
    .kpi > small { color: var(--muted); }
    .kpi.reputation { background: linear-gradient(135deg, var(--brown-700), var(--brown-500)); border-color: transparent; }
    .kpi.reputation span, .kpi.reputation small { color: #fff; }
    .badges { display: flex; flex-wrap: wrap; gap: 0.5rem; }
    .badge {
      display: inline-flex; align-items: center; gap: 0.3rem;
      font-size: 0.78rem; background: #f2e9db; color: var(--brown-700);
      border: 1px solid var(--border); border-radius: 999px; padding: 2px 10px;
    }
    .badge svg { width: 13px; height: 13px; }
    .hint { color: var(--muted); font-size: 0.88rem; margin: 0; }

    /* ---- Sections ---- */
    .section-head { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin: 2.25rem 0 0.85rem; }
    .section-head h2 { margin: 0; }
    .empty {
      background: var(--surface); border: 1px dashed var(--border); border-radius: var(--radius);
      padding: 1.25rem; text-align: center; color: var(--muted);
    }
    .spaced { margin-bottom: 0.85rem; }

    /* ---- Product form ---- */
    #product-form { margin-bottom: 1rem; }
    #product-form h3 { margin-bottom: 0.85rem; }
    .product-form { display: grid; grid-template-columns: 1fr 1fr; gap: 0.85rem 1rem; }
    .product-form .full { grid-column: 1 / -1; }
    @media (max-width: 600px) { .product-form { grid-template-columns: 1fr; } }

    /* ---- Product list ---- */
    .product-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.6rem; }
    .product-row {
      display: grid; grid-template-columns: 56px minmax(0, 1fr) auto auto; align-items: center; gap: 1rem;
      background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius);
      padding: 0.6rem 0.85rem;
    }
    .product-row.withdrawn { background: #fdf6f5; border-color: #f0c4bf; }
    .thumb {
      width: 56px; height: 56px; border-radius: 8px; overflow: hidden; background: #f2e9db;
      display: flex; align-items: center; justify-content: center; color: var(--muted);
    }
    .thumb img { width: 100%; height: 100%; object-fit: cover; }
    .thumb svg { width: 22px; height: 22px; }
    .info { display: flex; flex-direction: column; align-items: flex-start; gap: 0.15rem; min-width: 0; }
    .info a { color: inherit; }
    .info a:hover { color: var(--gold-dark); }
    .price { color: var(--gold-dark); white-space: nowrap; }
    .row-actions { display: flex; gap: 0.4rem; }
    .row-actions button { padding: 0.4rem 0.55rem; display: inline-flex; }
    .row-actions svg { width: 16px; height: 16px; }
    @media (max-width: 600px) {
      .product-row { grid-template-columns: 48px minmax(0, 1fr) auto; }
      .thumb { width: 48px; height: 48px; }
      .price { display: none; }
    }

    /* ---- Reviews ---- */
    .reviews { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.85rem; }
    .review header { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; }
    .review .date { margin-left: auto; }
    .stars { color: var(--gold); letter-spacing: 2px; }
    .comment { margin: 0.6rem 0 0.85rem; }
    .review > button { margin-top: 0.25rem; }
    .reply-form { margin-top: 0.75rem; }
    .link {
      border: none; background: transparent; padding: 0; color: var(--brown-600);
      font-size: 0.85rem; text-decoration: underline;
    }
    .link:hover { background: transparent; color: var(--gold-dark); }
  `,
})
export class MiTienda implements OnInit {
  private fb = inject(FormBuilder);
  private catalog = inject(CatalogService);
  private auth = inject(AuthService);
  private reputationService = inject(ReputationService);

  shop = signal<Shop | null>(null);
  stats = signal<SellerStats | null>(null);
  reputation = signal<Reputation | null>(null);
  reputationPending = signal(false);
  categories = signal<Category[]>([]);
  products = signal<Product[]>([]);
  reviews = signal<Review[]>([]);

  editingShop = signal(false);
  shopError = signal('');

  showForm = signal(false);
  editingId = signal<number | null>(null);
  error = signal('');
  listError = signal('');

  replyingId = signal<number | null>(null);
  replyError = signal('');
  replyForm = this.fb.nonNullable.group({
    reply: ['', [Validators.required, Validators.maxLength(1000)]],
  });

  unanswered = computed(() => this.reviews().filter((r) => !r.sellerReply).length);

  shopForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', [Validators.maxLength(2000)]],
  });

  form = this.fb.nonNullable.group({
    title: ['', [Validators.required]],
    description: [''],
    price: [0, [Validators.required, Validators.min(0)]],
    stock: [0, [Validators.required, Validators.min(0)]],
    categoryId: ['', [Validators.required]],
    images: [''],
  });

  ngOnInit() {
    this.catalog.myShop().subscribe((s) => {
      this.shop.set(s);
      this.loadReputation(s.id);
    });
    this.catalog.categories().subscribe((c) => this.categories.set(c));
    this.loadStats();
    this.reload();
    this.loadReviews();
  }

  private loadReputation(shopId: number) {
    this.reputationService.getReputation(shopId).subscribe({
      next: (rep) => this.reputation.set(rep),
      // 404 until the first recalculation has run for this shop.
      error: () => this.reputationPending.set(true),
    });
  }

  private loadStats() {
    this.auth.profileStats().subscribe((s) => this.stats.set(s.seller));
  }

  private loadReviews() {
    this.catalog.sellerReviews().subscribe((r) => this.reviews.set(r));
  }

  reload() {
    this.catalog.myProducts().subscribe((p) => this.products.set(p));
  }

  badgeLabel(badge: Badge): string {
    return BADGE_LABELS[badge] ?? badge;
  }

  stars(rating: number) {
    return '★'.repeat(rating) + '☆'.repeat(5 - rating);
  }

  // ── Shop ──────────────────────────────────────────────────────────

  startEditShop(shop: Shop) {
    this.shopForm.setValue({ name: shop.name, description: shop.description ?? '' });
    this.shopError.set('');
    this.editingShop.set(true);
  }

  saveShop() {
    if (this.shopForm.invalid) return;
    const v = this.shopForm.getRawValue();
    this.catalog.updateShop({ name: v.name.trim(), description: v.description.trim() || null }).subscribe({
      next: (s) => {
        this.shop.set(s);
        this.editingShop.set(false);
      },
      error: () => this.shopError.set('No se pudieron guardar los cambios de la tienda'),
    });
  }

  // ── Products ──────────────────────────────────────────────────────

  startCreate() {
    this.cancelEdit();
    this.showForm.set(true);
  }

  submit() {
    if (this.form.invalid) return;
    this.error.set('');
    const v = this.form.getRawValue();
    const req = {
      title: v.title,
      description: v.description || null,
      price: Number(v.price),
      stock: Number(v.stock),
      categoryId: Number(v.categoryId),
      images: v.images ? v.images.split(',').map((s) => s.trim()).filter(Boolean) : [],
    };
    const id = this.editingId();
    const op = id ? this.catalog.updateProduct(id, req) : this.catalog.createProduct(req);
    op.subscribe({
      next: () => {
        this.cancelEdit();
        this.reload();
        this.loadStats();
      },
      error: () => this.error.set('No se pudo guardar el producto'),
    });
  }

  edit(p: Product) {
    this.editingId.set(p.id);
    this.error.set('');
    this.form.setValue({
      title: p.title,
      description: p.description ?? '',
      price: p.price,
      stock: p.stock,
      categoryId: String(p.categoryId),
      images: p.images.join(', '),
    });
    this.showForm.set(true);
    setTimeout(() => document.getElementById('product-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }

  cancelEdit() {
    this.editingId.set(null);
    this.showForm.set(false);
    this.error.set('');
    this.form.reset({ title: '', description: '', price: 0, stock: 0, categoryId: '', images: '' });
  }

  remove(p: Product) {
    if (!confirm(`¿Eliminar "${p.title}"?`)) return;
    this.listError.set('');
    this.catalog.deleteProduct(p.id).subscribe({
      next: () => {
        this.reload();
        this.loadStats();
      },
      error: (e) =>
        this.listError.set(
          e.status === 409
            ? `"${p.title}" ya tiene pedidos y no se puede eliminar. Pon su stock a 0 para dejar de venderlo.`
            : 'No se pudo eliminar el producto',
        ),
    });
  }

  // ── Review replies ────────────────────────────────────────────────

  startReply(r: Review) {
    this.replyForm.setValue({ reply: r.sellerReply ?? '' });
    this.replyError.set('');
    this.replyingId.set(r.id);
  }

  sendReply(r: Review) {
    if (this.replyForm.invalid) return;
    this.catalog.replyToReview(r.id, this.replyForm.getRawValue().reply.trim()).subscribe({
      next: (updated) => {
        this.reviews.update((list) => list.map((x) => (x.id === updated.id ? updated : x)));
        this.replyingId.set(null);
      },
      error: () => this.replyError.set('No se pudo publicar la respuesta'),
    });
  }
}
