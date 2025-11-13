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
  designation = '';
  department = '';
  password = '';
  termsAccepted = false;
  filteredProjects: string[] = [];

  // Dynamic lists
  departments: string[] = [];
  projects: string[] = [];
  roles: any[] = [];

  constructor(private router: Router, private http: HttpClient, private dataService: DataService) {}

  ngOnInit(): void {
    this.loadDropdownData();
  }

  loadDropdownData(): void {
    this.dataService.getDepartments().subscribe({
      next: (data) => this.departments = data,
      error: (err) => console.error('Error loading departments:', err)
    });
  
    this.dataService.getRoles().subscribe({
      next: (data) => {
        console.log('Roles received:', data);
        this.roles = data;
      },
      error: (err) => console.error('Error loading roles:', err)
    });
  }

  loadAllProjects(): void {
    this.http.get<string[]>('http://localhost:8080/signup/projects').subscribe({
      next: (data) => {
        this.projects = data;
        this.filteredProjects = data;
      },
      error: (err) => {
        console.error('Failed to load projects:', err);
        this.projects = [];
        this.filteredProjects = [];
      }
    });
  }

  onDepartmentChange(): void {
    if (this.department) {
      this.dataService.getProjects(this.department).subscribe({
        next: (data) => this.projects = data,
        error: (err) => console.error('Error loading projects:', err)
      });
    }
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
      designation: this.designation
    };

    this.http.post('http://localhost:8080/create-user', signupData).subscribe({
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