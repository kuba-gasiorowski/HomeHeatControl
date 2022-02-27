import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, NEVER, throwError } from 'rxjs';
import { AuthService } from '../../auth/auth.service';

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
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required],
    });
  }

  login() {
    this.errorMessage = '';
    this.loginButtonDisabled = true;
    const val = this.loginForm.value;
    if (val.username && val.password) {
      this.auth.login(val.username, val.password).subscribe({
        next: () => {
          this.loginButtonDisabled = false;
          this.router.navigateByUrl('/', { skipLocationChange: true });
        },
        error: (err) => {
          this.errorMessage = err.error.error;
          this.loginButtonDisabled = false;
        },
      });
    }
  }

  ngOnInit(): void {}
}
