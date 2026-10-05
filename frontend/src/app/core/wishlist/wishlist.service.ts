import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Product } from '../catalog/catalog.service';

/** The buyer's favourite products. Keeps the saved ids in memory so any page can paint the heart. */
@Injectable({ providedIn: 'root' })
export class WishlistService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/me/wishlist`;
  private loaded = false;

  readonly ids = signal<ReadonlySet<number>>(new Set());

  list(): Observable<Product[]> {
    return this.http.get<Product[]>(this.base).pipe(
      tap((products) => {
        this.ids.set(new Set(products.map((p) => p.id)));
        this.loaded = true;
      }),
    );
  }

  /** Fetches the saved ids once per session. */
  ensureLoaded(): void {
    if (!this.loaded) this.list().subscribe({ error: () => {} });
  }

  has(productId: number): boolean {
    return this.ids().has(productId);
  }

  toggle(productId: number): Observable<void> {
    const saved = this.has(productId);
    const req = saved
      ? this.http.delete<void>(`${this.base}/${productId}`)
      : this.http.put<void>(`${this.base}/${productId}`, {});
    return req.pipe(
      tap(() =>
        this.ids.update((ids) => {
          const next = new Set(ids);
          if (saved) next.delete(productId);
          else next.add(productId);
          return next;
        }),
      ),
    );
  }

  clear(): void {
    this.ids.set(new Set());
    this.loaded = false;
  }
}
