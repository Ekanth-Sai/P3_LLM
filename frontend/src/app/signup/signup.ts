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
  designation = '';
  manager = '';
  password = '';   // <-- add this
  termsAccepted = false;
  departments: string[] = [];
  selectedDepartment = '';
  newDepartmentName = '';

  projects: string[] = [];
  selectedProject = '';
  newProjectName = '';

  constructor(private router: Router, private http: HttpClient) { }
  ngOnInit() {
    this.loadDepartments();
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
      password: this.password,    // <-- include password
      project: this.selectedProject,
      department:this.selectedDepartment,
      designation: this.designation,
      manager: this.manager
    };

    this.http.post('http://localhost:8080/signup/create-user', signupData).subscribe({
      next: (res: any) => {
        alert('Signup successful. Status: ' + res.message);
        this.router.navigate(['/login']);
      }
      ,
      error: (err: any) => {
        console.error(err);
        alert('Signup failed');
      }
    });
  }

  loadDepartments() {
    this.http.get<string[]>('http://localhost:8080/signup/departments').subscribe({
      next: (data) => (this.departments = data),
      error: (err) => {
        console.error('Failed to load departments:', err);
        this.departments = [];
      }
    });
  }

  loadProjects() {
    if (!this.selectedDepartment) return;
    this.http
      .get<string[]>(`http://localhost:8080/signup/projects/${this.selectedDepartment}`)
      .subscribe({
        next: (data) => (this.projects = data),
        error: (err) => {
          console.error('Failed to load projects:', err);
          this.projects = [];
        }
      });
  }


  onDepartmentChange() {
    this.selectedProject = '';
    //this.newProjectName = '';
    if (this.selectedDepartment) {
      setTimeout(() => this.loadProjects(), 0);
    } else {
      this.projects = [];
    }
  }
}
