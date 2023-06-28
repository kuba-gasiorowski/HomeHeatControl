import { Component, Input, OnInit } from '@angular/core';
import { FormatterService } from '../../helpers/formatter.service';
import { BackendApiService } from '../../shared/backend-api.service';
import { CircuitData } from '../../models/circuit-heating-data';
import { ConfirmDialogService } from '../confirm-dialog/confirm-dialog.service';
import { CircuitMode } from 'src/app/models/api/heat-status';

@Component({
  selector: '[app-circuit-data]',
  templateUrl: './circuit-data.component.html',
  styleUrls: ['./circuit-data.component.css'],
})
export class CircuitDataComponent implements OnInit {
  @Input()
  index: number = -1;

  @Input()
  circuitData: CircuitData = new CircuitData();

  lastCircuitMode: CircuitMode = CircuitMode.OFF;

  constructor(
    private formatterService: FormatterService,
    private confirmDialogService: ConfirmDialogService,
    public backendApiService: BackendApiService
  ) {}

  ngOnInit(): void {
    this.lastCircuitMode = this.circuitData.active;
  }

  confirmChangeActive(event: Event, circuitMode: CircuitMode): void {
    this.backendApiService
      .updateCircuit(this.circuitData.index, circuitMode)
      .subscribe((result) => {
        if (result.active !== undefined) {
          this.circuitData.active = result.active;
          this.backendApiService.lastStatus.circuitStatuses[
            this.index
          ].circuitStatus = result.active;
          this.lastCircuitMode = result.active;
        }
      });
  }

  cancelChangeActive(event: Event): void {
    this.circuitData.active = this.lastCircuitMode;
    event.preventDefault();
  }

  changeActive(event: Event) {
    const mode = (<HTMLInputElement>event.target).value;
    const circuitMode: CircuitMode = CircuitMode[mode as keyof typeof CircuitMode];
    const message =
      'Confirm change status of circuit ' +
      this.circuitData.index +
      ' to ' + mode;
    this.confirmDialogService.doConfirm(
      message,
      () => {
        this.confirmChangeActive(event, circuitMode);
      },
      () => {
        this.cancelChangeActive(event);
      }
    );
  }

  getTemperature(): string {
    if (this.circuitData.index >= 0) {
      return this.formatterService.formatTemperature(
        this.circuitData.temperature
      );
    } else {
      return '';
    }
  }
}
