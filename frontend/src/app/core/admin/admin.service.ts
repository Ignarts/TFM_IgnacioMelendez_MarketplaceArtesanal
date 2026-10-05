import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Category } from '../catalog/catalog.service';
import { ReportTargetType } from '../report/report.service';

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

export interface AdminReview {
  id: number;
  productId: number;
  productTitle: string;
  buyerName: string;
  rating: number;
  comment: string;
  createdAt: string;
}

export interface AdminReport {
  id: number;
  type: ReportTargetType;
  targetId: number;
  productId: number;
  productTitle: string;
  /** Only set for review reports. */
  reviewAuthor: string | null;
  reviewRating: number | null;
  reviewComment: string | null;
  reason: string;
  reporterName: string;
  createdAt: string;
}

export interface AdminProduct {
  id: number;
  title: string;
  shopId: number;
  shopName: string;
  hidden: boolean;
}

export interface CategoryRequest { name: string; slug?: string | null; }

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

  listReviews(): Observable<AdminReview[]> {
    return this.http.get<AdminReview[]>(`${this.base}/reviews`);
  }

  deleteReview(reviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/reviews/${reviewId}`);
  }

  // Reports
  listReports(): Observable<AdminReport[]> {
    return this.http.get<AdminReport[]>(`${this.base}/reports`);
  }

  dismissReport(reportId: number): Observable<AdminReport> {
    return this.http.post<AdminReport>(`${this.base}/reports/${reportId}/dismiss`, {});
  }

  // Product moderation
  listHiddenProducts(): Observable<AdminProduct[]> {
    return this.http.get<AdminProduct[]>(`${this.base}/products/hidden`);
  }

  hideProduct(productId: number): Observable<AdminProduct> {
    return this.http.post<AdminProduct>(`${this.base}/products/${productId}/hide`, {});
  }

  restoreProduct(productId: number): Observable<AdminProduct> {
    return this.http.post<AdminProduct>(`${this.base}/products/${productId}/restore`, {});
  }

  // Categories
  createCategory(req: CategoryRequest): Observable<Category> {
    return this.http.post<Category>(`${this.base}/categories`, req);
  }

  updateCategory(categoryId: number, req: CategoryRequest): Observable<Category> {
    return this.http.put<Category>(`${this.base}/categories/${categoryId}`, req);
  }

  deleteCategory(categoryId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/categories/${categoryId}`);
  }
}
