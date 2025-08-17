import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AdminService } from '../services/admin.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './admin.html',
  styleUrls: ['./admin.css']
})
  
export class AdminComponent implements OnInit {
  private adminService: AdminService = inject(AdminService);
  selectedOption: string = '';

  users: any[] = [];
  pendingUsers: any[] = [];
  view: 'dashboard' | 'existing' | 'pending' = 'dashboard';

  constructor(private router: Router) { }

  ngOnInit() {
    // initial dashboard view
    this.view = 'dashboard';
  }

  navigateToPage() {
    if (this.selectedOption === 'pending') {
      this.view = 'pending';
      this.loadPendingUsers();
    } else if (this.selectedOption === 'users') {
      this.view = 'existing';
      this.loadUsers();
    } else {
      this.view = 'dashboard';
    }
  }

  loadUsers() {
    this.adminService.getExistingUsers().subscribe(data => this.users = data);
  }

  loadPendingUsers() {
    this.adminService.getPendingUsers().subscribe(data => this.pendingUsers = data);
  }

  updateUser(user: any) {
    this.adminService.updateUser(user.id, { email: user.email, role: user.role }).subscribe(() => {
      alert('User updated!');
    });
  }

  handlePending(user: any, action: string) {
    let reason = '';
    if (action === 'decline') {
      reason = prompt('Reason for declining user?') || '';
    }
    this.adminService.handlePendingUser(user.id, action, reason).subscribe(() => {
      alert(`User ${action}ed!`);
      this.loadPendingUsers();
    });
  }

  goBackToDashboard() {
    this.view = 'dashboard';
    this.selectedOption = '';
  }
}
