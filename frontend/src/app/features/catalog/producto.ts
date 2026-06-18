import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CatalogService, Product } from '../../core/catalog/catalog.service';

@Component({
  selector: 'app-producto',
  imports: [RouterLink, DecimalPipe],
  template: `
    @if (product(); as p) {
      <p><a routerLink="/">← Volver al explorador</a></p>
      <h1>{{ p.title }}</h1>
      <p class="price">{{ p.price | number: '1.2-2' }} €</p>
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
    } @else if (notFound()) {
      <p>Producto no encontrado.</p>
    } @else {
      <p>Cargando…</p>
    }
  `,
})
export class Producto implements OnInit {
  private catalog = inject(CatalogService);
  private route = inject(ActivatedRoute);

  product = signal<Product | null>(null);
  notFound = signal(false);

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.catalog.product(id).subscribe({
      next: (p) => this.product.set(p),
      error: () => this.notFound.set(true),
    });
  }
}
