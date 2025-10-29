import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { HistoryService } from '../services/history.service';
import { AdminService } from '../services/admin.service';
import { DocumentService } from '../services/document.service';

@Component({
  selector: 'app-bot-usage',
  standalone: true,
  imports: [FormsModule, CommonModule, HttpClientModule],
  templateUrl: './bot-usage.html',
  styleUrls: ['./bot-usage.css'],
  providers: [HistoryService, AdminService, DocumentService] 
})
  
export class BotUsageComponent implements OnInit {
  private http = inject(HttpClient);
  private historyService = inject(HistoryService);
  private adminService = inject(AdminService);
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

  // ngOnInit() {
  //   const username = localStorage.getItem('email');
  //   if (username) {
  //     this.adminService.isAdmin(username).subscribe(response => {
  //       this.isAdmin = response.is_admin;
  //     });
  //   }
  // }

  ngOnInit() {
    const email = localStorage.getItem('email');
    this.username = email || 'User';

    if (email) {
      this.adminService.isAdmin(email).subscribe(response => {
        this.isAdmin = response.is_admin;
      });
    } 
  }

  sendMessage() {
    if (this.newMessage.trim() === '') return;

    this.messages.push({ text: this.newMessage, isUser: true });

    const username = localStorage.getItem('email');
    if (!username) {
      alert('Error: User not logged in.');
      return;
    }
    this.http.post('http://localhost:5001/query', { username, query: this.newMessage })
      .subscribe((response: any) => {
        this.messages.push({ text: response.response, isUser: false });
      });

    this.newMessage = '';
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

  showDocuments() {
    if (!this.isAdmin) return;
    
    this.activeTab = 'documents';
    this.loadingDocuments = true;
    this.deleteMessage = null;

    // this.documentService.getDocuments().subscribe(documents => {
    //   this.documents = documents;
    // });

    this.http.get<any[]>('http://localhost:8080/admin/files').subscribe({
      next: (files) => {
        this.documents = files;
        this.loadingDocuments = false;
      },
      error: (err) => {
        console.error('Error loading documents: ', err);
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
        this.deleteMessage = `Successfully deleted "${filename}" from the knowledge base.`;
        this.deleteSuccess = true;

        this.showDocuments();

        setTimeout(() => this.deleteMessage = null, 3000);
      },
      error: (err) => {
        console.error('Error deleting document: ', err);
        this.deleteMessage = `Failed to delete "${filename}".`;
        this.deleteSuccess = false;

        setTimeout(() => this.deleteMessage = null, 3000);
      }
    });
  }

  showChat() {
    this.activeTab = 'chat';
  }
}
