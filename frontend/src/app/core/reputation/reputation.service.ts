import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type Badge = 'VERIFIED' | 'FEATURED' | 'FAST_RESPONSE' | 'OVER_100_SALES';

export interface Reputation {
  shopId: number;
  score: number;
  nSales: number;
  avgRating: number;
  ageDays: number;
  incidentRate: number;
  badges: Badge[];
  updatedAt: string;
}

export const BADGE_LABELS: Record<Badge, string> = {
  VERIFIED: 'Verificado',
  FEATURED: 'Artesano destacado',
  FAST_RESPONSE: 'Respuesta rápida',
  OVER_100_SALES: '+100 ventas',
};

@Injectable({ providedIn: 'root' })
export class ReputationService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getReputation(shopId: number): Observable<Reputation> {
    return this.http.get<Reputation>(`${this.base}/shops/${shopId}/reputation`);
  }
}
