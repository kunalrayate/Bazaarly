import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const token = localStorage.getItem('token');
  const r = token && !req.url.includes('/auth/') ? req.clone({ setHeaders: { Authorization: 'Bearer ' + token } }) : req;
  return next(r).pipe(catchError(e => {
    if (e.status === 401 && token && !req.url.includes('/auth/')) { localStorage.removeItem('token'); localStorage.removeItem('user'); router.navigate(['/login'], { queryParams: { returnUrl: router.url } }).then(() => location.reload()); }
    return throwError(() => e);
  }));
};

/** Route guard: requires login and (optionally) one of the given roles. */
export const roleGuard = (...roles: string[]): CanActivateFn => (_route, state) => {
  const router = inject(Router);
  const user = JSON.parse(localStorage.getItem('user') || 'null');
  if (!user) return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  if (roles.length && !roles.includes(user.role)) return router.createUrlTree(['/']);
  return true;
};
