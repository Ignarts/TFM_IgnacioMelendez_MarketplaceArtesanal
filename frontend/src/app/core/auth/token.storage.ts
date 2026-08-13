import { Injectable, signal } from '@angular/core';

const KEY = 'jwt';

// ponytail: localStorage is the M0 choice (documented XSS trade-off). HttpOnly cookie if it matters.
@Injectable({ providedIn: 'root' })
export class TokenStorage {
  // Backed by a signal so consumers like AuthService.isLoggedIn (a computed()) actually
  // re-evaluate when the token changes — a plain localStorage read has no reactive dependency.
  private readonly token = signal<string | null>(localStorage.getItem(KEY));

  get(): string | null {
    return this.token();
  }
  set(token: string): void {
    localStorage.setItem(KEY, token);
    this.token.set(token);
  }
  clear(): void {
    localStorage.removeItem(KEY);
    this.token.set(null);
  }
}
