import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import {
  LucideChartColumn,
  LucideCircleUser,
  LucideCompass,
  LucideHeart,
  LucideLogIn,
  LucideLogOut,
  LucidePackage,
  LucideShieldCheck,
  LucideShoppingCart,
  LucideStore,
  LucideUserPlus,
} from '@lucide/angular';
import { AuthService } from './core/auth/auth.service';
import { OrderService } from './core/order/order.service';
import { WishlistService } from './core/wishlist/wishlist.service';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    LucideChartColumn,
    LucideCircleUser,
    LucideCompass,
    LucideHeart,
    LucideLogIn,
    LucideLogOut,
    LucidePackage,
    LucideShieldCheck,
    LucideShoppingCart,
    LucideStore,
    LucideUserPlus,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  auth = inject(AuthService);
  cart = inject(OrderService);
  private router = inject(Router);
  private wishlist = inject(WishlistService);

  isSeller = computed(() => this.auth.user()?.roles.includes('SELLER') ?? false);
  isAdmin = computed(() => this.auth.user()?.roles.includes('ADMIN') ?? false);

  constructor() {
    // Load roles for the navbar when a session already exists (e.g. after a reload).
    if (this.auth.isLoggedIn() && !this.auth.user()) {
      this.auth.me().subscribe();
    }
  }

  logout() {
    this.auth.logout();
    this.wishlist.clear();
    this.router.navigate(['/']);
  }
}
