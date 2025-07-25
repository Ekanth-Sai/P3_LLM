import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [RouterModule, CommonModule, FormsModule],
  templateUrl: './signup.html',
  styleUrls: ['./signup.css']
})

export class SignupComponent {
  constructor(private router: Router) { }
  
  onSignupSubmit(): void {
    //add validation logic
    this.router.navigate(['/signup-confirmation']);
  }
}
