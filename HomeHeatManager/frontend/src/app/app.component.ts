import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth/auth.service';
import { BackendApiService } from './shared/backend-api.service';
import { ConfirmDialogService } from './components/confirm-dialog/confirm-dialog.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'Home Heating Manager';

  constructor(
    private backendApiService: BackendApiService,
    private authService: AuthService,
    private confirmDialogService: ConfirmDialogService,
    private router: Router
  ) {}

  logout(): void {
    this.confirmDialogService.doConfirm(
      'Do you want to logout?',
      () => {
        this.authService.logout()?.subscribe();
        this.router.navigate(['/login'], { skipLocationChange: true });
      },
      () => {}
    );
  }

  isLogged(): boolean {
    return this.authService.isLogged;
  }

  ngOnInit(): void {}

  ngOnDestroy(): void {
    this.backendApiService.onDestroy();
  }
}
