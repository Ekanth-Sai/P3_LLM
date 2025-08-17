import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SignupService } from '../services/signup.service';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [RouterModule, CommonModule, FormsModule],
  templateUrl: './signup.html',
  styleUrls: ['./signup.css']
})
  
export class SignupComponent {
  firstName = '';
  lastName = '';
  email = '';
  project = '';
  designation = '';
  manager = '';
  password = '';   // <-- add this
  termsAccepted = false;

  constructor(private router: Router, private http: HttpClient) { }

  onSignupSubmit(): void {
    if (!this.termsAccepted) {
      alert('You must accept the terms and conditions');
      return;
    }

    const signupData = {
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      password: this.password,    // <-- include password
      project: this.project,
      designation: this.designation,
      manager: this.manager
    };

    this.http.post('http://localhost:8080/signup', signupData).subscribe({
      next: (res: any) => {
        alert('Signup successful. Status: ' + res.status);
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error(err);
        alert('Signup failed');
      }
    });
  }
}
