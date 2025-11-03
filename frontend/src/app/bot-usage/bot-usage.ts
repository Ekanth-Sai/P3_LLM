import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { HistoryService } from '../services/history.service';
import { AdminService } from '../services/admin.service';
import { Router } from '@angular/router';
import { DocumentService } from '../services/document.service';

@Component({
  selector: 'app-bot-usage',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './bot-usage.html',
  styleUrls: ['./bot-usage.css'],
  providers: [HistoryService, AdminService, DocumentService] 
})
export class BotUsageComponent implements OnInit {
  private http = inject(HttpClient);
  private historyService = inject(HistoryService);
  private adminService = inject(AdminService);
  private router = inject(Router);

  // private documentService = inject(DocumentService);

  messages: { text: string, isUser: boolean }[] = [];
  newMessage: string = '';
  history: any[] = [];
  documents: any[] = [];
  activeTab: string = 'chat';
  isAdmin: boolean = false;
  username: string = '';

  loadingDocuments: boolean = false;
  deleteMessage: string | null = null;
  deleteSuccess: boolean = false;

  /** 👇 NEW STATE for loader animation */
  isGeneratingResponse: boolean = false;

  ngOnInit() {
    const email = localStorage.getItem('email');
    this.username = email || 'User';
  
    if (email) {

      this.adminService.isAdmin(email).subscribe({
        next: (response) => {
          this.isAdmin = response.is_admin;
          console.log('Admin check:', response); 
        },
        error: (err) => {
          console.error('Admin check failed:', err);
        }
      });
    }
  }
  onLogout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('email');
    this.router.navigate(['/login']);
  }

  goBackToDashboard() {
    this.router.navigate(['/admin']);
  }

  sendMessage() {
    if (this.newMessage.trim() === '') return;

    const userInput = this.newMessage;
    this.messages.push({ text: userInput, isUser: true });
    this.newMessage = '';
    this.isGeneratingResponse = true; // show loader

    const username = localStorage.getItem('email');
    if (!username) {
      alert('Error: User not logged in.');
      this.isGeneratingResponse = false;
      return;
    }

    this.http.post('http://localhost:5001/query', { username, query: userInput })
      .subscribe({
        next: (response: any) => {
          this.messages.push({ text: response.response, isUser: false });
          this.isGeneratingResponse = false; // hide loader
        },
        error: (err) => {
          console.error('Error fetching response:', err);
          this.messages.push({ text: '⚠️ Something went wrong. Try again later.', isUser: false });
          this.isGeneratingResponse = false; // hide loader
        }
      });
  }

  showHistory() {
    this.activeTab = 'history';
    const username = localStorage.getItem('email');
    if (!username) {
      alert('Error: User not logged in.');
      return;
    }
    this.historyService.getHistory(username).subscribe(history => {
      this.history = history;
    });
  }

  

  groupedFiles: { [project: string]: any[] } = {};
  projectNames: string[] = [];
  expandedProjects: { [project: string]: boolean } = {};

  showDocuments() {
    if (!this.isAdmin) return;

    this.activeTab = 'documents';
    this.loadingDocuments = true;

    this.http.get<{ [project: string]: any[] }>('http://localhost:8080/admin/files').subscribe({
      next: (data) => {
        this.groupedFiles = data;
        this.projectNames = Object.keys(data);
        this.expandedProjects = this.projectNames.reduce((acc, p) => ({ ...acc, [p]: false }), {});
        this.loadingDocuments = false;
      },
      error: (err) => {
        console.error('Error loading documents:', err);
        this.loadingDocuments = false;
      }
    });
  }

  toggleProject(project: string) {
    this.expandedProjects[project] = !this.expandedProjects[project];
  }



  deleteDocument(filename: string) {
    if (!confirm(`Are you sure you want to delete "${filename}" from the knowledge base?`)) {
      return;
    }
  
    this.http.delete(`http://localhost:8080/admin/deleteFileByName/${filename}`).subscribe({
      next: () => {
        this.deleteMessage = `Successfully deleted "${filename}" from the knowledge base.`;
        this.deleteSuccess = true;
        this.showDocuments();
        setTimeout(() => this.deleteMessage = null, 3000);
      },
      error: (err) => {
        console.error('Error deleting document:', err);
        this.deleteMessage = `Failed to delete "${filename}".`;
        this.deleteSuccess = false;
        setTimeout(() => this.deleteMessage = null, 3000);
      }
    });
  }

  downloadFile(file: any) {
    const filename = file.filename || file; 
  
    this.http.get(`http://localhost:8080/admin/download-file/${filename}`, {
      responseType: 'blob',
      observe: 'response'
    }).subscribe({
      next: (response) => {
        const contentDisposition = response.headers.get('content-disposition');
        const nameFromHeader = contentDisposition
          ? contentDisposition.split('filename=')[1]?.replace(/"/g, '')
          : filename;
  
        const blob = response.body;
        if (blob) {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = nameFromHeader || 'downloaded_file';
          a.click();
          window.URL.revokeObjectURL(url);
        }
      },
      error: (err) => {
        console.error('❌ Error downloading file:', err);
        alert('Download failed. Please try again later.');
      }
    });
  }
  
  

  showChat() {
    this.activeTab = 'chat';
  }
}
