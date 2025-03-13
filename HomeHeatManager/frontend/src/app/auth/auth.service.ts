import { Injectable } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpHeaders,
} from '@angular/common/http';
import { UserCreds } from './user-creds';
import { AuthData } from './auth-data';
import { map, NEVER, Observable, shareReplay } from 'rxjs';
import { BackendApiService } from '../shared/backend-api.service';
import { Router } from '@angular/router';
import { environment } from 'src/environments/environment';
import {Utils} from "../shared/utils";

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  authUrl: string;
  isLogged: boolean = false;

  httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
    }),
  };

  constructor(
    private http: HttpClient,
    private backendApiService: BackendApiService,
    private router: Router,
  ) {
    this.authUrl = environment.authUrl;
  }

  login(username: string, password: string) {
    let creds: UserCreds = new UserCreds();
    creds.username = username;
    creds.password = password;

    return this.http
      .post<AuthData>(this.authUrl + '/login', creds, this.httpOptions)
      .pipe(
        map((authData) => {
          this.saveAuthData(authData);
        })
      )
      .pipe(shareReplay());
  }

  logout() {
    const refreshToken = window.sessionStorage.getItem('refresh_token');
    if (refreshToken) {
      this.removeAuthData();
      return this.http
        .post(this.authUrl + '/logout', refreshToken)
        .pipe(shareReplay());
    } else {
      return null;
    }
  }

  private saveAuthData(authData: AuthData): void {
    Utils.storeAuthData(authData);
    this.isLogged = true;
    this.backendApiService.onStart();
  }

  removeAuthData(): void {
    Utils.removeAuthData();
    this.isLogged = false;
    this.backendApiService.onDestroy();
    this.router.navigate(['login'], { skipLocationChange: true });
  }

  refreshToken(): Observable<any> {
    const refreshToken = Utils.getRefreshToken();
    if (refreshToken) {
      return this.http
        .post<AuthData>(this.authUrl + '/refresh', refreshToken)
        .pipe(
          map((authData) => this.saveAuthData(authData))
        )
        .pipe(shareReplay());
    } else {
      this.removeAuthData();
      throw new Error('Invalid token');
    }
  }
}
