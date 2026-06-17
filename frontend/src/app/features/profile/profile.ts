import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-profile',
  template: `
    <h1>Mi perfil</h1>
    @if (auth.user(); as u) {
      <ul>
        <li><strong>Nombre:</strong> {{ u.name }}</li>
        <li><strong>Email:</strong> {{ u.email }}</li>
        <li><strong>Roles:</strong> {{ u.roles.join(', ') }}</li>
      </ul>
    } @else {
      <p>Cargando…</p>
    }
    <button (click)="logout()">Cerrar sesión</button>
  `,
})
export class Profile implements OnInit {
  auth = inject(AuthService);
  private router = inject(Router);

  ngOnInit() {
    this.auth.me().subscribe();
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
