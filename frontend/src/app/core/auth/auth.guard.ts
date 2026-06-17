import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStorage } from './token.storage';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(TokenStorage).get() ? true : router.createUrlTree(['/login']);
};
