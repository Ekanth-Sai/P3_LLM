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
  documents: any[] = [];
  view: 'dashboard' | 'existing' | 'pending' | 'documents' = 'dashboard';

  decliningUser: any = null;
  declineReason: string = '';

  message: string | null = null;
  messageType: 'success' | 'error' = 'success';

  loadingDocuments: boolean = false;
  selectedFile: File | null = null;

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
    } else if (page === 'documents') {
      this.view = 'documents';
      this.loadDocuments();
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

  loadDocuments() {
    this.loadingDocuments = true;
    this.message = null;
    
    this.http.get<any[]>('http://localhost:8080/admin/files').subscribe({
      next: (files) => {
        this.documents = files;
        this.loadingDocuments = false;
      },
      error: (err) => {
        console.error('Error loading documents:', err);
        this.showMessage('Failed to load documents', 'error');
        this.loadingDocuments = false;
        this.documents = [];
      }
    });
  }

  deleteDocument(filename: string) {
    if (!confirm(`Are you sure you want to delete "${filename}" from the knowledge base?`)) {
      return;
    }

    this.http.delete(`http://localhost:8080/admin/files/${filename}`).subscribe({
      next: () => {
        this.showMessage(`Successfully deleted "${filename}"`, 'success');
        this.loadDocuments(); // Reload the list
      },
      error: (err) => {
        console.error('Error deleting document:', err);
        this.showMessage(`Failed to delete "${filename}"`, 'error');
      }
    });
  }

  onLogout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('email');
    this.router.navigate(['/login']);
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

  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
  }

  onFileUpload(event: Event) {
    event.preventDefault();
    if (!this.selectedFile) {
      this.showMessage('Please select a file', 'error');
      return;
    }

    const formData = new FormData();
    formData.append('file', this.selectedFile);

    this.http.post('http://localhost:8080/admin/upload-file', formData, { responseType: 'text' })
      .subscribe({
        next: (res) => {
          console.log('✅ Upload response:', res);
          this.showMessage('File uploaded successfully!', 'success');
          this.selectedFile = null;
          // Clear the file input
          const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
          if (fileInput) fileInput.value = '';
          // Reload documents list
          this.loadDocuments();
        },
        error: (err) => {
          console.error('❌ Upload error:', err);
          this.showMessage('Upload failed', 'error');
        }
      });
  }

  downloadFile(fileId: number) {
    this.http.get(`http://localhost:8080/admin/download-file/${fileId}`, { 
      responseType: 'blob', 
      observe: 'response' 
    }).subscribe(response => {
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
}