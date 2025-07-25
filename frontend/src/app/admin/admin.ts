import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin',
  imports: [FormsModule],
  templateUrl: './admin.html',
  styleUrls: ['./admin.css']
})
export class AdminComponent {
  selectedOption: string = '';

  constructor(private router: Router) { }
  
  navigateToPage() {
    if (this.selectedOption === 'pending') {
      this.router.navigate(['/pending-requests']);
    } else if (this.selectedOption === 'users') {
      this.router.navigate(['/existing-users']);
    }
  }
}
