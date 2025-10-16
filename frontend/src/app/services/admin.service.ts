import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private http = inject(HttpClient);
  private javaApiUrl = 'http://localhost:8080/admin'; // Java backend for user management
  private pythonApiUrl = 'http://localhost:5001'; // Python backend for role checks

  isAdmin(username: string): Observable<{ is_admin: boolean }> {
    return this.http.get<{ is_admin: boolean }>(`${this.pythonApiUrl}/is-admin?username=${username}`);
  }

  getExistingUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.javaApiUrl}/users`);
  }

  getPendingUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.javaApiUrl}/pending-users`);
  }

  updateUser(id: number, updates: any): Observable<any> {
    return this.http.put(`${this.javaApiUrl}/users/${id}`, updates);
  }

  handlePendingUser(id: number, action: string, reason?: string): Observable<any> {
    return this.http.post(`${this.javaApiUrl}/pending-users/${id}`, { action, reason });
  }
}