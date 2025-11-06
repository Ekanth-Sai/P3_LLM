import { Component, OnInit, inject } from '@angular/core';
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
export class SignupComponent implements OnInit {
  firstName = '';
  lastName = '';
  email = '';
  project = '';
  designation = '';
  department = '';
  manager = '';
  password = '';
  termsAccepted = false;

  // Dynamic lists
  departments: string[] = [];
  projects: string[] = [];
  filteredProjects: string[] = [];

  constructor(private router: Router, private http: HttpClient) {}

  ngOnInit(): void {
    this.loadDepartments();
    this.loadAllProjects();
  }

  loadDepartments(): void {
    this.http.get<string[]>('http://localhost:8080/signup/departments').subscribe({
      next: (data) => {
        this.departments = data;
      },
      error: (err) => {
        console.error('Failed to load departments:', err);
        this.departments = [];
      }
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
    this.project = ''; // Reset project selection
    
    if (!this.department) {
      this.filteredProjects = this.projects;
      return;
    }

    // Filter projects by selected department
    this.http.get<string[]>(`http://localhost:8080/signup/projects/${this.department}`)
      .subscribe({
        next: (data) => {
          this.filteredProjects = data;
        },
        error: (err) => {
          console.error('Failed to filter projects:', err);
          this.filteredProjects = this.projects;
        }
      });
  }

  onSignupSubmit(): void {
    if (!this.termsAccepted) {
      alert('You must accept the terms and conditions');
      return;
    }

    if (!this.department) {
      alert('Please select a department');
      return;
    }

    if (!this.project) {
      alert('Please select a project');
      return;
    }

    const signupData = {
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      password: this.password,
      project: this.project,
      department: this.department,
      designation: this.designation,
      manager: this.manager
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