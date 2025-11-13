// src/app/login/login.ts
import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterModule, CommonModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
})
export class LoginComponent {
  email = '';
  password = '';
  passwordVisible = false;

  private router = inject(Router);
  private authService = inject(AuthService);

  togglePasswordVisibility() {
    this.passwordVisible = !this.passwordVisible;
  }

  loginWithSSO() {
    alert('SSO login is not implemented yet.');
  }

  onLogin() {
    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: (response: any) => {
        if (response.status === 'success') {
          // ✅ Store JWT token
          localStorage.setItem('token', response.token);

          // ✅ Store role & email (optional but useful)
          localStorage.setItem('role', response.role);
          localStorage.setItem('email', this.email);

          // ✅ Navigate based on role
          if (response.role === 'USER' || response.role === 'PRODUCT_MANAGER' || response.role === 'SENIOR_DEVELOPER' || response.role === 'DEVELOPER' || response.role === 'HR') {
            this.router.navigate(['/bot-usage']);
          } else if (response.role === 'ADMIN') {
            this.router.navigate(['/admin']);
          } else {
            alert('Unknown role');
          }
        } else {
          alert('Invalid credentials');
        }
      },
      error: (err: any) => {
        console.error('❌ Login error:', err);
        alert('Login failed');
      },
    });
  }
}
