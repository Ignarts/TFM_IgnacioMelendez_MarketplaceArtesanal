import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <h1>Iniciar sesión</h1>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <label>Email <input type="email" formControlName="email" /></label>
      <label>Contraseña <input type="password" formControlName="password" /></label>
      @if (error()) { <p class="error">{{ error() }}</p> }
      <button type="submit" [disabled]="form.invalid">Entrar</button>
    </form>
    <p>¿No tienes cuenta? <a routerLink="/register">Regístrate</a></p>
  `,
})
export class Login {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  error = signal('');
  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit() {
    if (this.form.invalid) return;
    this.error.set('');
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/profile']),
      error: (e) =>
        this.error.set(
          e.status === 403
            ? 'Tu cuenta está suspendida. Contacta con el equipo de soporte.'
            : 'Credenciales no válidas',
        ),
    });
  }
}
