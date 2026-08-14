import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenStorage } from './token.storage';

export interface RegisterRequest { email: string; password: string; name: string; }
export interface LoginRequest { email: string; password: string; }
export interface AuthResponse { token: string; type: string; email: string; roles: string[]; }
export interface Me { id: number; email: string; name: string; roles: string[]; }

export interface SellerStats {
  productsSold: number;
  reviewsReceived: number;
  avgRating: number;
  productsOnSale: number;
}
export interface ProfileStats {
  purchaseCount: number;
  uniqueProductsBought: number;
  starsGiven: number;
  seller: SellerStats | null;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private tokens = inject(TokenStorage);
  private base = environment.apiUrl;

  readonly user = signal<Me | null>(null);
  readonly isLoggedIn = computed(() => !!this.tokens.get());

  register(req: RegisterRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/auth/register`, req);
  }

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/auth/login`, req).pipe(
      tap((res) => this.tokens.set(res.token)),
    );
  }

  me(): Observable<Me> {
    return this.http.get<Me>(`${this.base}/me`).pipe(tap((me) => this.user.set(me)));
  }

  profileStats(): Observable<ProfileStats> {
    return this.http.get<ProfileStats>(`${this.base}/me/profile-stats`);
  }

  logout(): void {
    this.tokens.clear();
    this.user.set(null);
  }
}
