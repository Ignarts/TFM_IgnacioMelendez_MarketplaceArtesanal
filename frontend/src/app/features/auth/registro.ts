import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <h1>Crear cuenta</h1>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <label>Nombre <input formControlName="nombre" /></label>
      <label>Email <input type="email" formControlName="email" /></label>
      <label>Contraseña (mín. 8) <input type="password" formControlName="password" /></label>
      @if (error()) { <p class="error">{{ error() }}</p> }
      <button type="submit" [disabled]="form.invalid">Registrarme</button>
    </form>
    <p>¿Ya tienes cuenta? <a routerLink="/login">Inicia sesión</a></p>
  `,
})
export class Registro {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  error = signal('');
  form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit() {
    if (this.form.invalid) return;
    this.error.set('');
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/login']),
      error: (e) =>
        this.error.set(e.status === 409 ? 'Ese email ya está registrado' : 'No se pudo registrar'),
    });
  }
}
