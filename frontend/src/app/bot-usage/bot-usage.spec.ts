import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BotUsageComponent } from './bot-usage';

describe('BotUsageComponent', () => {
  let component: BotUsageComponent;
  let fixture: ComponentFixture<BotUsageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BotUsageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BotUsageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });
});
