import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { KnowledgeBaseComponent } from './knowledge-base';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

describe('KnowledgeBaseComponent', () => {
  let component: KnowledgeBaseComponent;
  let fixture: ComponentFixture<KnowledgeBaseComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KnowledgeBaseComponent, HttpClientTestingModule, RouterTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(KnowledgeBaseComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });
  

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should load departments on init', fakeAsync(() => {
    const mockDepartments = ['AI', 'Finance'];
    const req = httpMock.expectOne('http://localhost:8080/admin/departments');
    expect(req.request.method).toBe('GET');
    req.flush(mockDepartments);
    tick();

    expect(component.departments.length).toBe(2);
    expect(component.departments).toContain('AI');
  }));

  it('should load documents grouped by department and project', fakeAsync(() => {
    const mockFiles = {
      "AI": {
        "Vision": [{ filename: 'image1.pdf' }],
        "NLP": [{ filename: 'text1.docx' }]
      },
      "Finance": {
        "Reports": [{ filename: 'report1.xlsx' }]
      }
    };

    component.loadDocuments();
    const req = httpMock.expectOne('http://localhost:8080/admin/files');
    expect(req.request.method).toBe('GET');
    req.flush(mockFiles);
    tick();

    expect(Object.keys(component.groupedFiles).length).toBe(2);
    expect(component.groupedFiles['AI']['Vision'][0].filename).toBe('image1.pdf');
  }));

  it('should show a success message when showMessage is called', () => {
    component.showMessage('Uploaded successfully', 'success');
    expect(component.message).toBe('Uploaded successfully');
    expect(component.messageType).toBe('success');
  });

  it('should toggle department expansion correctly', () => {
    component.expandedDepartments = { AI: false };
    component.toggleDepartment('AI');
    expect(component.expandedDepartments['AI']).toBeTrue();
  });

  it('should toggle project expansion correctly', () => {
    const key = 'AI_Vision';
    component.expandedProjects = { [key]: false };
    component.toggleProject('AI', 'Vision');
    expect(component.expandedProjects[key]).toBeTrue();
  });

  it('should call upload endpoint with correct payload', fakeAsync(() => {
    const dummyFile = new File(['content'], 'data.pdf', { type: 'application/pdf' });
    component.selectedFile = dummyFile;
    component.selectedDepartment = 'AI';
    component.selectedProject = 'Vision';

    component.onFileUpload(new Event('submit'));

    const req = httpMock.expectOne('http://localhost:8080/admin/upload-file');
    expect(req.request.method).toBe('POST');
    req.flush({});
    tick();

    expect(component.messageType).toBe('success');
  }));

  it('should delete document successfully', fakeAsync(() => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.deleteDocument('data.pdf');
    const req = httpMock.expectOne('http://localhost:8080/admin/deleteFileByName/data.pdf');
    expect(req.request.method).toBe('DELETE');
    req.flush({});
    tick();

    expect(component.messageType).toBe('success');
  }));

  it('should reset projects when department changes', fakeAsync(() => {
    component.projects = ['OldProject'];
    component.selectedDepartment = 'AI';
    component.onDepartmentChange();
    expect(component.projects.length).toBe(0);
  }));
});

