import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CatalogService, Category, Product, Shop } from '../../core/catalog/catalog.service';

@Component({
  selector: 'app-mi-tienda',
  imports: [ReactiveFormsModule, DecimalPipe],
  template: `
    @if (shop(); as s) {
      <h1>{{ s.name }}</h1>
      <p>{{ s.description }}</p>
      <p><small>{{ s.verified ? 'Tienda verificada ✓' : 'Pendiente de verificación' }}</small></p>
    }

    <h2>{{ editingId() ? 'Editar producto' : 'Nuevo producto' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <label>Título <input formControlName="title" /></label>
      <label>Descripción <textarea formControlName="description"></textarea></label>
      <label>Precio <input type="number" formControlName="price" min="0" step="0.01" /></label>
      <label>Stock <input type="number" formControlName="stock" min="0" /></label>
      <label>Categoría
        <select formControlName="categoryId">
          <option value="">Selecciona…</option>
          @for (c of categories(); track c.id) { <option [value]="c.id">{{ c.name }}</option> }
        </select>
      </label>
      <label>Imágenes (URLs separadas por coma) <input formControlName="images" /></label>
      @if (error()) { <p class="error">{{ error() }}</p> }
      <button type="submit" [disabled]="form.invalid">{{ editingId() ? 'Guardar' : 'Crear' }}</button>
      @if (editingId()) { <button type="button" (click)="cancelEdit()">Cancelar</button> }
    </form>

    <h2>Mis productos</h2>
    @if (products().length === 0) {
      <p>Todavía no has publicado productos.</p>
    } @else {
      <ul>
        @for (p of products(); track p.id) {
          <li>
            {{ p.title }} — {{ p.price | number: '1.2-2' }} € (stock: {{ p.stock }})
            <button (click)="edit(p)">Editar</button>
            <button (click)="remove(p)">Eliminar</button>
          </li>
        }
      </ul>
    }
  `,
})
export class MiTienda implements OnInit {
  private fb = inject(FormBuilder);
  private catalog = inject(CatalogService);

  shop = signal<Shop | null>(null);
  categories = signal<Category[]>([]);
  products = signal<Product[]>([]);
  editingId = signal<number | null>(null);
  error = signal('');

  form = this.fb.nonNullable.group({
    title: ['', [Validators.required]],
    description: [''],
    price: [0, [Validators.required, Validators.min(0)]],
    stock: [0, [Validators.required, Validators.min(0)]],
    categoryId: ['', [Validators.required]],
    images: [''],
  });

  ngOnInit() {
    this.catalog.myShop().subscribe((s) => this.shop.set(s));
    this.catalog.categories().subscribe((c) => this.categories.set(c));
    this.reload();
  }

  reload() {
    this.catalog.myProducts().subscribe((p) => this.products.set(p));
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
      },
      error: () => this.error.set('No se pudo guardar el producto'),
    });
  }

  edit(p: Product) {
    this.editingId.set(p.id);
    this.form.setValue({
      title: p.title,
      description: p.description ?? '',
      price: p.price,
      stock: p.stock,
      categoryId: String(p.categoryId),
      images: p.images.join(', '),
    });
  }

  cancelEdit() {
    this.editingId.set(null);
    this.form.reset({ title: '', description: '', price: 0, stock: 0, categoryId: '', images: '' });
  }

  remove(p: Product) {
    if (!confirm(`¿Eliminar "${p.title}"?`)) return;
    this.catalog.deleteProduct(p.id).subscribe(() => this.reload());
  }
}
