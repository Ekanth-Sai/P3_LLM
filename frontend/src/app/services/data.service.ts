import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DataService {

  private baseUrl = 'http://localhost:8080/api/data';

  constructor(private http: HttpClient) { }

  getDepartments(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/departments`);
  }

  getProjects(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/projects`);
  }

  getRoles(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/roles`);
  }
}
