import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'app-bot-usage',
  standalone: true,
  imports: [FormsModule, CommonModule, HttpClientModule],
  templateUrl: './bot-usage.html',
  styleUrls: ['./bot-usage.css']
})
export class BotUsageComponent {
  private http = inject(HttpClient);
  messages: { text: string, isUser: boolean }[] = [];
  newMessage: string = '';

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
}
