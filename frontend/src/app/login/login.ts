import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

interface LoginResponse {
  token: string;
  role: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterModule, HttpClientModule, FormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
  
export class LoginComponent {
  passwordVisible = false;
  email: string = '';
  password: string = '';

  constructor(private router: Router, private http: HttpClient) { }

  togglePasswordVisibility() {
    this.passwordVisible = !this.passwordVisible;
  }

  onLogin() {
    if (!this.email || !this.password) {
      alert('Please enter both email and password');
      return;
    }

    const loginData = {
      email: this.email,
      password: this.password
    };

    console.log(loginData.email)
    console.log(loginData.password)

    this.http.post<LoginResponse>('http://localhost:8080/api/auth/login', loginData).subscribe({
      next: (response) => {
        if (response.token && response.role) {
          // Store token in localStorage
          localStorage.setItem('authToken', response.token);
          localStorage.setItem('userRole', response.role);
          
          console.log('Login successful, role:', response.role);
          
          // Navigate based on role
          if (response.role === 'ADMIN') {
            this.router.navigate(['/admin']);
          } else if (response.role === 'USER') {
            this.router.navigate(['/bot-usage']);
          } else {
            console.error('Unknown role:', response.role);
            alert('Unknown user role');
          }
        } else {
          alert('Login failed: Invalid response from server');
        }
      },
      error: (error) => {
        console.error('Login error: ', error);
        if (error.status === 400) {
          alert('Login failed: Invalid credentials');
        } else {
          alert('Login failed: ' + (error.error?.message || 'Server error'));
        }
      }
    });
  }
}