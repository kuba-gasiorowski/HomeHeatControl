import { Component, Inject, OnInit } from '@angular/core';
import { HeatingData } from '../../models/heating-data';
import { FormatterService } from '../../helpers/formatter.service';
import { BackendApiService } from '../../shared/backend-api.service';
import { HeatStatus } from '../../models/api/heat-status';

@Component({
  selector: 'app-general-heating-info',
  templateUrl: './general-heating-info.component.html',
  styleUrls: ['./general-heating-info.component.css'],
})
export class GeneralHeatingInfoComponent implements OnInit {
  public ManagementInterfaceStatusEnum = ManagementInterfaceStatus;

  heatData: HeatingData = new HeatingData();

  constructor(
    private formatterService: FormatterService,
    public backendApiService: BackendApiService
  ) {}

  displayClass(): string {
    switch (this.getManagementInterfaceStatus()) {
      case ManagementInterfaceStatus.ACTIVE:
        return 'bg-success';
      case ManagementInterfaceStatus.OUTDATED:
        return 'bg-warning';
      default:
        return 'bg-secondary';
    }
  }

  getManagementInterfaceStatus(): ManagementInterfaceStatus {
    if (this.heatData.ready) {
      if (
        Date.now() - this.heatData.lastMessageTime.getTime() >
        1000 * 60 * 5
      ) {
        return ManagementInterfaceStatus.OUTDATED;
      } else {
        return ManagementInterfaceStatus.ACTIVE;
      }
    } else {
      return ManagementInterfaceStatus.OFF;
    }
  }

  getExternalTemperature(): string {
    return this.formatterService.formatTemperature(
      this.heatData.externalTemperature
    );
  }

  getAverageExternalTemperature(): string {
    return this.formatterService.formatTemperature(
      this.heatData.averageExternalTemperature
    );
  }

  copyData(value: HeatStatus): void {
    if (value.ready) {
      this.heatData.controllerStatus = value.controllerStatus;
      this.heatData.lastStatusChangeTime = value.lastStatusChangeTime;
      this.heatData.lastMessageTime = value.lastMessageTime;
      this.heatData.heatingPeriod = value.heatingPeriod?.toLocaleString();
      this.heatData.externalTemperature = value.externalTemperature;
      this.heatData.averageExternalTemperature = value.avgExternalTemperature;
      this.heatData.ready = true;
    }
  }

  ngOnInit(): void {
    this.copyData(this.backendApiService.lastStatus);
    this.backendApiService.heatStatus.subscribe((value) => {
      this.copyData(value);
    });
  }
}

export enum ManagementInterfaceStatus {
  ACTIVE,
  OUTDATED,
  OFF,
}
