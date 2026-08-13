import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile implements OnInit {
  auth = inject(AuthService);
  private router = inject(Router);

  ngOnInit() {
    this.auth.me().subscribe();
  }

  initials(name: string): string {
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  }

  roleLabel(role: string): string {
    const labels: Record<string, string> = {
      ADMIN: 'Administrador',
      SELLER: 'Vendedor',
      BUYER: 'Comprador',
    };
    return labels[role] ?? role;
  }

  accountType(roles: string[]): string {
    if (roles.includes('ADMIN')) return 'Administrador';
    if (roles.includes('SELLER')) return 'Vendedor';
    return 'Comprador';
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
