import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SignupConfirmationComponent } from './signup-confirmation';

describe('SignupConfirmation', () => {
  let component: SignupConfirmationComponent;
  let fixture: ComponentFixture<SignupConfirmationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SignupConfirmationComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SignupConfirmationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
