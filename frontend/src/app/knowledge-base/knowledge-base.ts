import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-knowledge-base',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './knowledge-base.html',
  styleUrls: ['./knowledge-base.css']
})
export class KnowledgeBaseComponent implements OnInit {
  private http = inject(HttpClient);
  private router = inject(Router);

  departments: string[] = [];
  selectedDepartment = '';
  newDepartmentName = '';

  projects: string[] = [];
  selectedProject = '';
  newProjectName = '';

  selectedFile: File | null = null;
  selectedRoles: string[] = [];
  availableRoles: any[] = [];
  selectedSensitivity = 'Internal';

  groupedFiles: { [department: string]: { [project: string]: any[] } } = {};
  expandedDepartments: { [department: string]: boolean } = {};
  expandedProjects: { [key: string]: boolean } = {};

  message: string | null = null;
  messageType: 'success' | 'error' = 'success';
  loadingDocuments = false;

  Object = Object; 

  ngOnInit() {
    this.loadDepartments();
    this.loadDocuments();
    this.loadRoles();
  }

  showMessage(text: string, type: 'success' | 'error') {
    this.message = text;
    this.messageType = type;
    setTimeout(() => (this.message = null), 4000);
  }

  loadDepartments() {
    this.http.get<string[]>('http://localhost:8080/admin/departments').subscribe({
      next: (data) => (this.departments = data),
      error: (err) => {
        console.error('Failed to load departments:', err);
        this.departments = [];
      }
    });
  }

  loadProjects() {
    if (!this.selectedDepartment) return;
    this.http
      .get<string[]>(`http://localhost:8080/admin/projects/${this.selectedDepartment}`)
      .subscribe({
        next: (data) => (this.projects = data),
        error: (err) => {
          console.error('Failed to load projects:', err);
          this.projects = [];
        }
      });
  }

  onDepartmentChange() {
    this.selectedProject = '';
    this.newProjectName = '';
    if (this.selectedDepartment && this.selectedDepartment !== '__new__') {
      setTimeout(() => this.loadProjects(), 0);
    } else {
      this.projects = [];
    }
  }

  loadDocuments() {
    this.loadingDocuments = true;
    this.http
      .get<{ [department: string]: { [project: string]: any[] } }>(
        'http://localhost:8080/admin/files'
      )
      .subscribe({
        next: (data) => {
          this.groupedFiles = data;
          this.expandedDepartments = Object.keys(data).reduce(
            (acc, dep) => ({ ...acc, [dep]: false }),
            {}
          );
          this.loadingDocuments = false;
          console.log('✅ Loaded grouped files:', this.groupedFiles);
        },
        error: (err) => {
          console.error('❌ Error loading documents:', err);
          this.loadingDocuments = false;
        }
      });
  }

  toggleDepartment(department: string) {
    this.expandedDepartments[department] = !this.expandedDepartments[department];
  }

  toggleProject(department: string, project: string) {
    const key = `${department}_${project}`;
    this.expandedProjects[key] = !this.expandedProjects[key];
  }

  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
  }

  onFileUpload(event: Event) {
    event.preventDefault();

    if (!this.selectedFile) {
      this.showMessage('Please select a file first.', 'error');
      return;
    }

    const department =
      this.selectedDepartment === '__new__'
        ? this.newDepartmentName.trim()
        : this.selectedDepartment;

    const project =
      this.selectedProject === '__new__' ? this.newProjectName.trim() : this.selectedProject;

    if (!department) {
      this.showMessage('Please choose or enter a department.', 'error');
      return;
    }
    if (!project) {
      this.showMessage('Please choose or enter a project.', 'error');
      return;
    }

    // Expand roles to include parents before upload
    const expandedRoles = this.expandRolesToIncludeParents(this.selectedRoles);

    const formData = new FormData();
    formData.append('file', this.selectedFile);
    formData.append('departmentName', department);
    formData.append('projectName', project);
    formData.append('sensitivity', this.selectedSensitivity || 'Internal');
    formData.append('allowedRolesJson', expandedRoles.join(','));

    this.http.post('http://localhost:8080/admin/upload-file', formData).subscribe({
      next: () => {
        this.showMessage(`File uploaded to ${department} / ${project}`, 'success');
        this.loadDocuments();
        this.selectedFile = null;
        this.selectedRoles = [];
      },
      error: (err) => {
        console.error('Upload failed:', err);
        this.showMessage('Upload failed. Please try again.', 'error');
      }
    });
  }

  downloadFile(file: any) {
    const filename = file.filename || file;
    this.http
      .get(`http://localhost:8080/admin/download-file/${filename}`, {
        responseType: 'blob',
        observe: 'response'
      })
      .subscribe({
        next: (response) => {
          const contentDisposition = response.headers.get('content-disposition');
          const nameFromHeader = contentDisposition
            ? contentDisposition.split('filename=')[1]?.replace(/"/g, '')
            : filename;

          const blob = response.body;
          if (blob) {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = nameFromHeader || 'downloaded_file';
            a.click();
            window.URL.revokeObjectURL(url);
          }
        },
        error: (err) => {
          console.error('Download failed:', err);
          this.showMessage('Download failed. Please try again.', 'error');
        }
      });
  }

  deleteDocument(filename: string) {
    if (!confirm(`Are you sure you want to delete "${filename}"?`)) return;

    this.http.delete(`http://localhost:8080/admin/deleteFileByName/${filename}`).subscribe({
      next: () => {
        this.showMessage(`Deleted "${filename}" successfully!`, 'success');
        this.loadDocuments();
      },
      error: (err) => {
        console.error('Error deleting document:', err);
        this.showMessage(`Failed to delete "${filename}".`, 'error');
      }
    });
  }

  goBackToDashboard() {
    this.router.navigate(['/admin']);
  }

  onLogout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('email');
    this.router.navigate(['/login']);
  }

  loadRoles() {
    this.http.get<any[]>('http://localhost:8080/api/roles/all')
      .subscribe({
        next: (data) => {
          this.availableRoles = data;
          console.log('✅ Loaded roles:', this.availableRoles);
        },
        error: (err) => console.error('Failed to load roles:', err)
      });
  }

  /**
   * Expand selected roles to include all parent roles
   * For example: if "DEVELOPER" is selected, it will return ["DEVELOPER", "SENIOR_DEVELOPER", "TEAM_LEAD", "CTO", "CEO"]
   */
  expandRolesToIncludeParents(selectedRoles: string[]): string[] {
    const roleHierarchyMap: Record<string, string[]> = {
      'INTERN': ['INTERN', 'JUNIOR_DEVELOPER', 'DEVELOPER', 'SENIOR_DEVELOPER', 'TEAM_LEAD', 'CTO', 'CEO'],
      'JUNIOR_DEVELOPER': ['JUNIOR_DEVELOPER', 'DEVELOPER', 'SENIOR_DEVELOPER', 'TEAM_LEAD', 'CTO', 'CEO'],
      'DEVELOPER': ['DEVELOPER', 'SENIOR_DEVELOPER', 'TEAM_LEAD', 'CTO', 'CEO'],
      'SENIOR_DEVELOPER': ['SENIOR_DEVELOPER', 'TEAM_LEAD', 'CTO', 'CEO'],
      'TEAM_LEAD': ['TEAM_LEAD', 'CTO', 'CEO'],
      'CTO': ['CTO', 'CEO'],
      'PRODUCT_MANAGER': ['PRODUCT_MANAGER', 'CPO', 'CEO'],
      'PROJECT_MANAGER': ['PROJECT_MANAGER', 'CPO', 'CEO'],
      'CPO': ['CPO', 'CEO'],
      'CFO': ['CFO', 'CEO'],
      'HR': ['HR', 'CEO'],
      'CEO': ['CEO']
    };

    const expandedRoles = new Set<string>();

    for (const role of selectedRoles) {
      const hierarchy = roleHierarchyMap[role] || [role];
      hierarchy.forEach(r => expandedRoles.add(r));
    }

    return Array.from(expandedRoles);
  }
}