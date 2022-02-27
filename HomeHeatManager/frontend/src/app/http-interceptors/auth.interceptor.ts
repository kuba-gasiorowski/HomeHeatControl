import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse,
} from '@angular/common/http';
import {
  Subject,
  catchError,
  filter,
  Observable,
  of,
  switchMap,
  take,
  throwError,
  tap,
  NEVER,
} from 'rxjs';
import { AuthService } from '../auth/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  static AUTH_HEADER = 'Authorization';
  static JWT_PREFIX = 'Bearer ';

  private isRefreshing: boolean = false;
  private tokenRefreshSubject: Subject<any> = new Subject<any>();
  private tokenRefresh$ = this.tokenRefreshSubject.asObservable();

  constructor(private authService: AuthService) {}

  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    if (request.url.includes('/auth')) return next.handle(request);
    let authRequest = this.addAuthHeader(request);
    return next.handle(authRequest).pipe(
      catchError((err) => {
        if (
          err instanceof HttpErrorResponse &&
          !authRequest.url.includes('/auth/') &&
          err.status === 401
        ) {
          return this.handle401(authRequest, next);
        }
        return throwError(() => err);
      })
    );
  }

  /**
   * Adds authorization header with JWT token to the HTTP request or do nothing
   * if the user hasn't authenticated yet or the token is not valid
   * @param request The original HTTP request
   * @returns The HTTP request with Authorization header added if valid JWT token found
   */
  addAuthHeader(request: HttpRequest<any>): HttpRequest<any> {
    if (this.authService.isLogged) {
      let token = sessionStorage.getItem('access_token');
      if (token)
        return request.clone({
          headers: request.headers.set(
            AuthInterceptor.AUTH_HEADER,
            AuthInterceptor.JWT_PREFIX + token
          ),
        });
    }
    return request;
  }

  refreshToken() {
    if (this.isRefreshing) {
      return new Observable((observer) => {
        this.tokenRefresh$.subscribe(() => {
          observer.next();
          observer.complete();
        });
      });
    } else {
      this.isRefreshing = true;
      return this.authService.refreshToken().pipe(
        tap((res) => {
          this.isRefreshing = false;
          this.tokenRefreshSubject.next(res);
        }),
        catchError(() => {
          this.isRefreshing = false;
          this.authService.removeAuthData();

          return NEVER;
        })
      );
    }
  }

  handle401(request: HttpRequest<any>, next: HttpHandler) {
    return this.refreshToken().pipe(
      switchMap(() => {
        request = this.addAuthHeader(request);
        return next.handle(request);
      }),
      catchError((err) => {
        if (err.status === 401) {
          this.isRefreshing = false;
          this.authService.removeAuthData();
          return NEVER;
        } else {
          return throwError(() => err);
        }
      })
    );
  }
}
