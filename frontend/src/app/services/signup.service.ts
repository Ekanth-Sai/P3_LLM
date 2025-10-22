import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class SignupService {
    private baseUrl = 'http://localhost:8080/create-user'; // your Spring Boot endpoint
    
    constructor(private http: HttpClient) { }

    registerUser(data: any) {
        return this.http.post(this.baseUrl, data);
    }
}
