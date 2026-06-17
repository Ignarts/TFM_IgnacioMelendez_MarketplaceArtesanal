import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { TokenStorage } from './token.storage';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const tokens = inject(TokenStorage);
  const router = inject(Router);
  const token = tokens.get();

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      // ponytail: skip redirect for /auth/* so a bad login shows its error instead of looping.
      if (err.status === 401 && !req.url.includes('/auth/')) {
        tokens.clear();
        router.navigate(['/login']);
      }
      return throwError(() => err);
    }),
  );
};
