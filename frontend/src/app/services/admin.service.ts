import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface User {
    id: number;
    email: string;
    role: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
private baseUrl = 'http://localhost:8080/admin';

constructor(private http: HttpClient) {}

getExistingUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.baseUrl}/users`);
}

updateUser(id: number, updates: Partial<User>): Observable<any> {
    return this.http.put(`${this.baseUrl}/users/${id}`, updates);
}

getPendingUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.baseUrl}/pending-users`);
}

handlePendingUser(id: number, action: string, reason?: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/pending-users/${id}`, { action, reason });
}
}
