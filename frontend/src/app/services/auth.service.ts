import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
// import { AuthService } from '..';
import { Router } from "@angular/router";
import { Observable } from "rxjs";

interface LoginResponse {
    token: string;
    role: string;
}

@Injectable({
    providedIn: 'root'
})

export class AuthService { 
    private apiUrl = 'http://localhost:8080/api/auth';

    constructor(private http: HttpClient, private router: Router) { }
    
    login(email: string, password: string): Observable<LoginResponse> {
        return this.http.post<LoginResponse>(`${this.apiUrl}`, { email, password });
    }

    saveToken(token: string) {
        localStorage.setItem('authToken', token);
    }

    saveUserRole(role: string) {
        localStorage.setItem('userRole', role);
    }

    getToken(): string | null {
        return localStorage.getItem('authToken');
    }

    getUserRole(): string | null {
        return localStorage.getItem('userRole');
    }

    logout() {
        localStorage.removeItem('authToken');
        localStorage.removeItem('userRole');
        this.router.navigate(['/login']);
    }

    isLoggedIn(): boolean {
        return !!this.getToken();
    }

    isAdmin(): boolean {
        return this.getUserRole() === 'ADMIN';
    }

    isUser(): boolean {
        return this.getUserRole() === 'USER';
    }
}
