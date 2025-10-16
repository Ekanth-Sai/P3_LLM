import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/admin/files';

  getDocuments(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }
}
