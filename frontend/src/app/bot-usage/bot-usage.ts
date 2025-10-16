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
  providers: [HistoryService, AdminService, DocumentService] // Add services to providers
})
export class BotUsageComponent implements OnInit {
  private http = inject(HttpClient);
  private historyService = inject(HistoryService);
  private adminService = inject(AdminService);
  private documentService = inject(DocumentService);

  messages: { text: string, isUser: boolean }[] = [];
  newMessage: string = '';
  history: any[] = [];
  documents: any[] = [];
  activeTab: string = 'chat';
  isAdmin: boolean = false;

  ngOnInit() {
    const username = localStorage.getItem('email');
    if (username) {
      this.adminService.isAdmin(username).subscribe(response => {
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
    this.activeTab = 'documents';
    this.documentService.getDocuments().subscribe(documents => {
      this.documents = documents;
    });
  }

  showChat() {
    this.activeTab = 'chat';
  }
}
