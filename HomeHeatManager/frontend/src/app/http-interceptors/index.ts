import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { TypeInterceptor } from './type-interceptor';
import { AuthInterceptor } from './auth.interceptor';

export const httpInterceptorProviders = [
  { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
  { provide: HTTP_INTERCEPTORS, useClass: TypeInterceptor, multi: true },
];
