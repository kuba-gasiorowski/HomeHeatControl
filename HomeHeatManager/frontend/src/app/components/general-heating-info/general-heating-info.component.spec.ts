import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GeneralHeatingInfoComponent } from './general-heating-info.component';

describe('GeneralHeatingInfoComponent', () => {
  let component: GeneralHeatingInfoComponent;
  let fixture: ComponentFixture<GeneralHeatingInfoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GeneralHeatingInfoComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GeneralHeatingInfoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
