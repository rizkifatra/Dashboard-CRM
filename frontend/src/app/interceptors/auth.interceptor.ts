import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { Router } from '@angular/router';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  // Clone request and add Authorization header if token exists
  let authReq = req;
  if (token && !req.url.includes('/oauth2/') && !req.url.includes('/login/')) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  // Handle the request and catch authentication errors
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Handle 401 Unauthorized errors
      if (error.status === 401) {
        console.error(
          'Authentication failed - Token may be invalid or expired',
        );

        // Don't redirect if we're already on login or auth callback pages
        const currentUrl = window.location.pathname;
        if (
          !currentUrl.includes('/login') &&
          !currentUrl.includes('/auth-callback')
        ) {
          // Clear session and redirect to login
          localStorage.clear();
          sessionStorage.clear();
          router.navigate(['/login'], {
            queryParams: {
              returnUrl: currentUrl,
              reason: 'session_expired',
            },
          });
        }
      }

      // Handle 403 Forbidden errors
      if (error.status === 403) {
        console.error('Access forbidden - Insufficient permissions');
      }

      // Re-throw the error for component-level handling
      return throwError(() => error);
    }),
  );
};
