import { Component, OnInit } from '@angular/core';
import { HeatStatus } from '../../models/api/heat-status';
import { CircuitData } from '../../models/circuit-heating-data';
import { BackendApiService } from '../../shared/backend-api.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class DashboardComponent implements OnInit {
  circuitData: CircuitData[] = [];
  constructor(public backendApiService: BackendApiService) {}

  copyData(value: HeatStatus): void {
    if (value.ready) {
      this.circuitData = [];
      for (let i = 0; i < value.circuitStatuses.length; i++) {
        let circuitDataElem: CircuitData = new CircuitData();
        circuitDataElem.index = value.circuitStatuses[i].circuitIndex;
        circuitDataElem.name = value.circuitStatuses[i].circuitName;
        circuitDataElem.temperature =
          value.circuitStatuses[i].circuitTemperature;
        circuitDataElem.heating = value.circuitStatuses[i].heatingOn;
        circuitDataElem.active = value.circuitStatuses[i].circuitStatus;
        this.circuitData.push(circuitDataElem);
      }
    }
  }

  ngOnInit(): void {
    this.copyData(this.backendApiService.lastStatus);
    this.backendApiService.heatStatus.subscribe((result) => {
      this.copyData(result);
    });
  }
}
