import { Component, Input, OnInit } from '@angular/core';
import { FormatterService } from '../../helpers/formatter.service';
import { BackendApiService } from '../../shared/backend-api.service';
import { CircuitData } from '../../models/circuit-heating-data';
import { ConfirmDialogService } from '../confirm-dialog/confirm-dialog.service';

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

  constructor(
    private formatterService: FormatterService,
    private confirmDialogService: ConfirmDialogService,
    public backendApiService: BackendApiService
  ) {}

  ngOnInit(): void {}

  confirmChangeActive(isChecked: boolean): void {
    this.backendApiService
      .updateCircuit(this.circuitData.index, isChecked)
      .subscribe((result) => {
        if (typeof result.active === 'boolean') {
          this.circuitData.active = result.active;
          this.backendApiService.lastStatus.circuitStatuses[
            this.index
          ].circuitStatus = result.active;
        }
      });
  }

  cancelChangeActive(event: Event, isChecked: boolean): void {
    this.circuitData.active = !isChecked;
    event.preventDefault();
  }

  changeActive(event: Event) {
    const isChecked = (<HTMLInputElement>event.target).checked;
    const message =
      'Confirm change status of circuit ' +
      this.circuitData.index +
      ' to ' +
      (isChecked ? 'active' : 'disabled');
    this.confirmDialogService.doConfirm(
      message,
      () => {
        this.confirmChangeActive(isChecked);
      },
      () => {
        this.cancelChangeActive(event, isChecked);
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
