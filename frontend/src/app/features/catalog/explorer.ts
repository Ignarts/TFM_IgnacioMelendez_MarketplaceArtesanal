import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CatalogService, Category, Product } from '../../core/catalog/catalog.service';

@Component({
  selector: 'app-explorer',
  imports: [ReactiveFormsModule, RouterLink, DecimalPipe],
  template: `
    <h1>Explorar productos</h1>

    <form [formGroup]="filters" (ngSubmit)="search()" class="filters">
      <input formControlName="q" placeholder="Buscar…" />
      <select formControlName="categoryId">
        <option value="">Todas las categorías</option>
        @for (c of categories(); track c.id) {
          <option [value]="c.id">{{ c.name }}</option>
        }
      </select>
      <input type="number" formControlName="minPrice" placeholder="Precio mín." min="0" />
      <input type="number" formControlName="maxPrice" placeholder="Precio máx." min="0" />
      <button type="submit">Filtrar</button>
    </form>

    @if (products().length === 0) {
      <p>No hay productos que coincidan.</p>
    } @else {
      <ul class="grid">
        @for (p of products(); track p.id) {
          <li>
            <a [routerLink]="['/producto', p.id]">
              @if (p.images.length) { <img [src]="p.images[0]" [alt]="p.title" /> }
              <h3>{{ p.title }}</h3>
              <p>{{ p.price | number: '1.2-2' }} €</p>
              <small>{{ p.shopName }} · {{ p.categoryName }}</small>
            </a>
          </li>
        }
      </ul>
    }
  `,
})
export class Explorer implements OnInit {
  private catalog = inject(CatalogService);
  private fb = inject(FormBuilder);

  categories = signal<Category[]>([]);
  products = signal<Product[]>([]);

  filters = this.fb.nonNullable.group({
    q: '',
    categoryId: '',
    minPrice: '',
    maxPrice: '',
  });

  ngOnInit() {
    this.catalog.categories().subscribe((c) => this.categories.set(c));
    this.search();
  }

  search() {
    const v = this.filters.getRawValue();
    this.catalog
      .products({
        q: v.q || undefined,
        categoryId: v.categoryId ? Number(v.categoryId) : undefined,
        minPrice: v.minPrice ? Number(v.minPrice) : undefined,
        maxPrice: v.maxPrice ? Number(v.maxPrice) : undefined,
      })
      .subscribe((p) => this.products.set(p));
  }
}
