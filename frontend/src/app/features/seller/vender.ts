import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CatalogService } from '../../core/catalog/catalog.service';

@Component({
  selector: 'app-vender',
  imports: [ReactiveFormsModule],
  template: `
    <h1>Abre tu tienda</h1>
    <p>Conviértete en vendedor y empieza a publicar tus productos artesanales.</p>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <label>Nombre de la tienda <input formControlName="name" /></label>
      <label>Descripción <textarea formControlName="description"></textarea></label>
      @if (error()) { <p class="error">{{ error() }}</p> }
      <button type="submit" [disabled]="form.invalid">Crear tienda</button>
    </form>
  `,
})
export class Vender {
  private fb = inject(FormBuilder);
  private catalog = inject(CatalogService);
  private auth = inject(AuthService);
  private router = inject(Router);

  error = signal('');
  form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    description: [''],
  });

  submit() {
    if (this.form.invalid) return;
    this.error.set('');
    this.catalog.openShop(this.form.getRawValue()).subscribe({
      // Refresh roles so the SELLER routes/guards become available without re-login.
      next: () => this.auth.me().subscribe(() => this.router.navigate(['/mi-tienda'])),
      error: (e) =>
        this.error.set(e.status === 409 ? 'Ya tienes una tienda' : 'No se pudo crear la tienda'),
    });
  }
}
