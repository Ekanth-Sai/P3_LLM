import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

export const TokenInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();
  //console.log(`[TokenInterceptor] ${req.url}`, 'token?', !!token);
  if (token) {
    if (authService.isTokenExpired()) {
      authService.logout();
      router.navigate(['/login']);
      return next(req); // stop request after logout
    }

    const cloned = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
    return next(cloned);
  }

  return next(req);
};
