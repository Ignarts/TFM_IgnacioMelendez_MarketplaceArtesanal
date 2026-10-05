import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucidePencil, LucidePlus, LucideTrash2 } from '@lucide/angular';
import { AdminService, CategoryRequest } from '../../core/admin/admin.service';
import { CatalogService, Category } from '../../core/catalog/catalog.service';

@Component({
  selector: 'app-admin-categories',
  imports: [ReactiveFormsModule, LucidePencil, LucidePlus, LucideTrash2],
  template: `
    <form class="inline-form" [formGroup]="createForm" (ngSubmit)="create()">
      <input formControlName="name" placeholder="Nombre de la categoría" maxlength="60" />
      <input formControlName="slug" placeholder="Slug (opcional, se genera solo)" maxlength="60" />
      <button type="submit" [disabled]="createForm.invalid"><svg lucidePlus></svg> Añadir</button>
    </form>

    @if (message()) { <p class="success">{{ message() }}</p> }
    @if (error()) { <p class="error">{{ error() }}</p> }

    <div class="table-wrap">
      <table class="data-table">
        <thead><tr><th>Nombre</th><th>Slug</th><th></th></tr></thead>
        <tbody>
          @for (c of categories(); track c.id) {
            @if (editingId() === c.id) {
              <tr [formGroup]="editForm">
                <td><input formControlName="name" maxlength="60" /></td>
                <td><input formControlName="slug" maxlength="60" /></td>
                <td class="actions">
                  <button (click)="saveEdit(c)" [disabled]="editForm.invalid">Guardar</button>
                  <button type="button" (click)="editingId.set(null)">Cancelar</button>
                </td>
              </tr>
            } @else {
              <tr>
                <td>{{ c.name }}</td>
                <td><code>{{ c.slug }}</code></td>
                <td class="actions">
                  <button type="button" (click)="startEdit(c)" aria-label="Editar"><svg lucidePencil></svg></button>
                  <button type="button" class="danger" (click)="remove(c)" aria-label="Eliminar"><svg lucideTrash2></svg></button>
                </td>
              </tr>
            }
          }
        </tbody>
      </table>
    </div>
  `,
  styles: `
    .inline-form { flex-direction: row; flex-wrap: wrap; max-width: none; gap: 0.6rem; margin-bottom: 1rem; }
    .inline-form input { flex: 1 1 200px; }
    .inline-form button { display: inline-flex; align-items: center; gap: 0.35rem; }
    .success, .error { margin-bottom: 1rem; }
    td input { width: 100%; padding: 0.35rem 0.55rem; }
    .actions button { display: inline-flex; align-items: center; gap: 0.35rem; }
    .actions svg, .inline-form svg { width: 15px; height: 15px; }
    code { font-size: 0.82rem; color: var(--muted); }
  `,
})
export class AdminCategories implements OnInit {
  private fb = inject(FormBuilder);
  private adminService = inject(AdminService);
  private catalog = inject(CatalogService);

  categories = signal<Category[]>([]);
  editingId = signal<number | null>(null);
  message = signal('');
  error = signal('');

  createForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(60)]],
    slug: [''],
  });

  editForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(60)]],
    slug: [''],
  });

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.catalog.categories().subscribe((c) => this.categories.set(c));
  }

  private toRequest(v: { name: string; slug: string }): CategoryRequest {
    return { name: v.name.trim(), slug: v.slug.trim() || null };
  }

  private ok(text: string): void {
    this.error.set('');
    this.message.set(text);
    this.load();
  }

  private fail(e: { status: number }, conflictText: string): void {
    this.message.set('');
    this.error.set(e.status === 409 ? conflictText : 'No se pudo guardar la categoría.');
  }

  create(): void {
    if (this.createForm.invalid) return;
    const req = this.toRequest(this.createForm.getRawValue());
    this.adminService.createCategory(req).subscribe({
      next: (c) => {
        this.createForm.reset();
        this.ok(`Categoría "${c.name}" creada.`);
      },
      error: (e) => this.fail(e, 'Ya existe una categoría con ese slug.'),
    });
  }

  startEdit(category: Category): void {
    this.editForm.setValue({ name: category.name, slug: category.slug });
    this.editingId.set(category.id);
  }

  saveEdit(category: Category): void {
    if (this.editForm.invalid) return;
    this.adminService.updateCategory(category.id, this.toRequest(this.editForm.getRawValue())).subscribe({
      next: (c) => {
        this.editingId.set(null);
        this.ok(`Categoría "${c.name}" actualizada.`);
      },
      error: (e) => this.fail(e, 'Ya existe una categoría con ese slug.'),
    });
  }

  remove(category: Category): void {
    if (!confirm(`¿Eliminar la categoría "${category.name}"?`)) return;
    this.adminService.deleteCategory(category.id).subscribe({
      next: () => this.ok(`Categoría "${category.name}" eliminada.`),
      error: (e) =>
        this.fail(e, `No se puede eliminar "${category.name}": todavía tiene productos. Reasígnalos antes.`),
    });
  }
}
