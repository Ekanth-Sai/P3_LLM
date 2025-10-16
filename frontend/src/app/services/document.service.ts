import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:5001/processed-documents';

  getDocuments(): Observable<any[]> {
    return this.http.get<string[]>(this.apiUrl).pipe(
      map(paths => paths.map(path => ({ filename: path.split('/').pop() })))
    );
  }
}
