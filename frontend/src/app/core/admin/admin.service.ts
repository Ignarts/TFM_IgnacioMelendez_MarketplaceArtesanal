import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AdminShop {
  id: number;
  ownerId: number;
  name: string;
  verified: boolean;
  createdAt: string;
}

export interface AdminUser {
  id: number;
  email: string;
  name: string;
  roles: string[];
  suspended: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/admin`;

  listShops(): Observable<AdminShop[]> {
    return this.http.get<AdminShop[]>(`${this.base}/shops`);
  }

  listPendingShops(): Observable<AdminShop[]> {
    return this.http.get<AdminShop[]>(`${this.base}/shops/pending`);
  }

  verifyShop(shopId: number): Observable<AdminShop> {
    return this.http.post<AdminShop>(`${this.base}/shops/${shopId}/verify`, {});
  }

  listUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.base}/users`);
  }

  suspendUser(userId: number): Observable<AdminUser> {
    return this.http.post<AdminUser>(`${this.base}/users/${userId}/suspend`, {});
  }

  unsuspendUser(userId: number): Observable<AdminUser> {
    return this.http.post<AdminUser>(`${this.base}/users/${userId}/unsuspend`, {});
  }

  deleteReview(reviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/reviews/${reviewId}`);
  }
}
