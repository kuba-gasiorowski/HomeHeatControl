import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CircuitDataComponent } from './circuit-data.component';

describe('CircuitDataComponent', () => {
  let component: CircuitDataComponent;
  let fixture: ComponentFixture<CircuitDataComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CircuitDataComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CircuitDataComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
