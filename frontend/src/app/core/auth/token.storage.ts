import { Injectable } from '@angular/core';

const KEY = 'jwt';

// ponytail: localStorage is the M0 choice (documented XSS trade-off). HttpOnly cookie if it matters.
@Injectable({ providedIn: 'root' })
export class TokenStorage {
  get(): string | null {
    return localStorage.getItem(KEY);
  }
  set(token: string): void {
    localStorage.setItem(KEY, token);
  }
  clear(): void {
    localStorage.removeItem(KEY);
  }
}
