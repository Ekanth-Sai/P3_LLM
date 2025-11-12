import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DataService {

  private adminBase = 'http://localhost:8080/signup';
  private rolesBase = 'http://localhost:8080/api/roles';

  constructor(private http: HttpClient) {}

  getDepartments(): Observable<string[]> {
    return this.http.get<string[]>(`${this.adminBase}/departments`);
  }

  getProjects(department?: string): Observable<string[]> {
    if (department) {
      return this.http.get<string[]>(`${this.adminBase}/projects/${department}`);
    } else {
      return this.http.get<string[]>(`${this.adminBase}/projects`);
    }
  }

  getRoles(): Observable<any[]> {
    return this.http.get<any[]>(`${this.rolesBase}/all`);
  }
}
