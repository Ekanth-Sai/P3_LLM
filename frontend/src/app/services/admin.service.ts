import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { HttpHeaders } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private http = inject(HttpClient);
  private javaApiUrl = 'http://localhost:8080/admin'; // Java backend for user management
  private pythonApiUrl = 'http://localhost:5001'; // Python backend for role checks

  isAdmin(email: string): Observable<any> {
    const params = { email }; 
    const headers = new HttpHeaders(); 
    return this.http.get<any>(`${this.javaApiUrl}/is-admin?email=${email}`);
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