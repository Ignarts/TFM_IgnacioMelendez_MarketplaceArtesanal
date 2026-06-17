import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenStorage } from './token.storage';

export interface RegisterRequest { email: string; password: string; nombre: string; }
export interface LoginRequest { email: string; password: string; }
export interface AuthResponse { token: string; tipo: string; email: string; roles: string[]; }
export interface Me { id: number; email: string; nombre: string; roles: string[]; }

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

  logout(): void {
    this.tokens.clear();
    this.user.set(null);
  }
}
