import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';
import { TokenStorage } from './token.storage';

/** Allows access only to logged-in users that hold the given role. */
export function roleGuard(role: string): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!inject(TokenStorage).get()) return router.createUrlTree(['/login']);

    const decide = (roles: string[]) => (roles.includes(role) ? true : router.createUrlTree(['/']));

    const user = auth.user();
    if (user) return decide(user.roles);

    // Roles not loaded yet (e.g. after a page reload): fetch them from /me.
    return auth.me().pipe(
      map((me) => decide(me.roles)),
      catchError(() => of(router.createUrlTree(['/login']))),
    );
  };
}
