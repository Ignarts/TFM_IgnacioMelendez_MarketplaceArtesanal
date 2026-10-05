import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type ReportTargetType = 'PRODUCT' | 'REVIEW';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  report(type: ReportTargetType, targetId: number, reason: string): Observable<void> {
    return this.http.post<void>(`${this.base}/reports`, { type, targetId, reason });
  }
}
