import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { ModalLoadingService } from '../modal-loading/modal-loading.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  errorMessage: string = '';
  loginButtonDisabled = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private loadingService: ModalLoadingService
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required],
    });
  }

  login() {
    this.loadingService.invokeLoading(true);
    this.errorMessage = '';
    this.loginButtonDisabled = true;
    const val = this.loginForm.value;
    if (val.username && val.password) {
      this.auth.login(val.username, val.password).subscribe({
        next: () => {
          this.loadingService.invokeLoading(false);
          this.loginButtonDisabled = false;
          this.router.navigateByUrl('/', { skipLocationChange: true });
        },
        error: (err) => {
          this.loadingService.invokeLoading(false);
          this.errorMessage = err.error.error;
          this.loginButtonDisabled = false;
        },
      });
    }
  }

  ngOnInit(): void {
    this.loadingService.invokeLoading(false);
  }
}
