import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Category { id: number; name: string; slug: string; }

export interface Product {
  id: number;
  shopId: number;
  shopName: string;
  categoryId: number;
  categoryName: string;
  title: string;
  description: string | null;
  price: number;
  stock: number;
  images: string[];
}

export interface Shop { id: number; ownerId: number; name: string; description: string | null; verified: boolean; }

export interface ProductRequest {
  title: string;
  description?: string | null;
  price: number;
  stock: number;
  categoryId: number;
  images?: string[];
}

export interface ProductFilters { q?: string; categoryId?: number; minPrice?: number; maxPrice?: number; }

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  // Public catalog
  products(filters: ProductFilters = {}): Observable<Product[]> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    }
    return this.http.get<Product[]>(`${this.base}/products`, { params });
  }

  product(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.base}/products/${id}`);
  }

  categories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.base}/categories`);
  }

  // Seller shop
  openShop(req: { name: string; description?: string }): Observable<Shop> {
    return this.http.post<Shop>(`${this.base}/seller/shop`, req);
  }

  myShop(): Observable<Shop> {
    return this.http.get<Shop>(`${this.base}/seller/shop`);
  }

  myProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.base}/seller/products`);
  }

  createProduct(req: ProductRequest): Observable<Product> {
    return this.http.post<Product>(`${this.base}/seller/products`, req);
  }

  updateProduct(id: number, req: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.base}/seller/products/${id}`, req);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/seller/products/${id}`);
  }
}
