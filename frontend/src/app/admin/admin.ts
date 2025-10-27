import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AdminService } from '../services/admin.service';
import { HttpClient } from '@angular/common/http';

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
  files: any[] = [];
  view: 'dashboard' | 'existing' | 'pending' = 'dashboard';

  decliningUser: any = null;
  declineReason: string = '';

  message: string | null = null;
  messageType: 'success' | 'error' = 'success';

  constructor(private router: Router, private http: HttpClient) { }

  ngOnInit() {
    this.view = 'dashboard';
  }

  showMessage(msg: string, type: 'success' | 'error' = 'success') {
    this.message = msg;
    this.messageType = type;
    setTimeout(() => this.message = null, 5000);
  }

  navigateTo(page: string) {
    if (page === 'pending') {
      this.view = 'pending';
      this.loadPendingUsers();
    } else if (page === 'users') {
      this.view = 'existing';
      this.loadUsers();
    } else if (page === 'bot-usage') {
      this.router.navigate(['/bot-usage']);
    } else {
      this.view = 'dashboard';
    }
  }

  loadUsers() {
    this.adminService.getExistingUsers().subscribe((data: any[]) => this.users = data);
  }

  loadPendingUsers() {
    this.adminService.getPendingUsers().subscribe((data: any[]) => this.pendingUsers = data);
  }

  updateUser(user: any) {
    this.adminService.updateUser(user.id, { email: user.email, role: user.role }).subscribe(
        () => this.showMessage('User updated successfully!'),
        () => this.showMessage('Failed to update user.', 'error')
    );
  }

  handlePending(user: any, action: string) {
    if (action === 'decline') {
      this.decliningUser = user;
      this.declineReason = '';
    } else {
      this.adminService.handlePendingUser(user.id, action).subscribe(() => {
        this.showMessage(`User ${action}ed!`);
        this.loadPendingUsers();
      });
    }
  }

  confirmDecline() {
    if (!this.decliningUser) return;

    this.adminService.handlePendingUser(this.decliningUser.id, 'decline', this.declineReason).subscribe(() => {
      this.showMessage('User declined!');
      this.loadPendingUsers();
      this.cancelDecline();
    });
  }

  cancelDecline() {
    this.decliningUser = null;
    this.declineReason = '';
  }

  goBackToDashboard() {
    this.view = 'dashboard';
    this.selectedOption = '';
  }

  selectedFile: File | null = null;

  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
  }

  onFileUpload(event: Event) {
    event.preventDefault();
    if (!this.selectedFile) return;

    const formData = new FormData();
    formData.append('file', this.selectedFile);

    this.http.post('http://localhost:8080/admin/upload-file', formData).subscribe(
      () => this.showMessage('File uploaded!'),
      () => this.showMessage('Upload failed', 'error')
    );
  }

  downloadFile(fileId: number) {
    this.http.get(`http://localhost:8080/admin/download-file/${fileId}`, { responseType: 'blob', observe: 'response' })
      .subscribe(response => {
        const contentDisposition = response.headers.get('content-disposition');
        const filename = contentDisposition?.split(';')[1].split('filename=')[1].split('"')[1] || 'downloaded_file';

        const blob = response.body;
        if (blob) {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            a.click();
            window.URL.revokeObjectURL(url);
        }
      });
  }

  loadFiles() {
    this.http.get<any[]>('http://localhost:8080/admin/files').subscribe(data => this.files = data);
  }
}