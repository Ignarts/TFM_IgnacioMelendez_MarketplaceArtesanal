import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  auth = inject(AuthService);
  private router = inject(Router);

  isSeller = computed(() => this.auth.user()?.roles.includes('SELLER') ?? false);

  constructor() {
    // Load roles for the navbar when a session already exists (e.g. after a reload).
    if (this.auth.isLoggedIn() && !this.auth.user()) {
      this.auth.me().subscribe();
    }
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/']);
  }
}
