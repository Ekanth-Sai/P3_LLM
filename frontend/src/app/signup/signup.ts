import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { DataService } from '../services/data.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [RouterModule, CommonModule, FormsModule],
  templateUrl: './signup.html',
  styleUrls: ['./signup.css']
})
export class SignupComponent implements OnInit {
  firstName = '';
  lastName = '';
  email = '';
  project = '';
  role = '';
  department = '';
  password = '';
  termsAccepted = false;

  // Dynamic lists
  departments: string[] = [];
  projects: string[] = [];
  roles: string[] = [];

  constructor(private router: Router, private http: HttpClient, private dataService: DataService) {}

  ngOnInit(): void {
    this.loadDropdownData();
  }

  loadDropdownData(): void {
    this.dataService.getDepartments().subscribe(data => this.departments = data);
    this.dataService.getProjects().subscribe(data => this.projects = data);
    this.dataService.getRoles().subscribe(data => this.roles = data);
  }

  onSignupSubmit(): void {
    if (!this.termsAccepted) {
      alert('You must accept the terms and conditions');
      return;
    }

    const signupData = {
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      password: this.password,
      project: this.project,
      department: this.department,
      role: this.role
    };

    this.http.post('http://localhost:8080/signup', signupData).subscribe({
      next: (res: any) => {
        alert('Signup successful. Awaiting admin approval.');
        this.router.navigate(['/signup-confirmation']);
      },
      error: (err) => {
        console.error(err);
        alert('Signup failed: ' + (err.error?.message || 'Unknown error'));
      }
    });
  }
}