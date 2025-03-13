import { Component, OnInit } from '@angular/core';
import { HeatStatus } from '../../models/api/heat-status';
import { CircuitData } from '../../models/circuit-heating-data';
import { BackendApiService } from '../../shared/backend-api.service';
import { ModalLoadingService } from '../modal-loading/modal-loading.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class DashboardComponent implements OnInit {
  circuitData: CircuitData[] = [];
  private loading = true;
  constructor(public backendApiService: BackendApiService,
    private loadingService: ModalLoadingService) {}

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
    if (!this.backendApiService.lastStatus.ready) {
      this.loadingService.invokeLoading(true);
      this.backendApiService.getStatus().subscribe(next => {
        this.copyData(next);
        if (this.loading) {
          this.loadingService.invokeLoading(false);
          this.loading = false;
        }
      });
    }
    this.copyData(this.backendApiService.lastStatus);
    this.backendApiService.heatStatus.subscribe((result) => {
      this.copyData(result);
      if (this.loading) {
        this.loadingService.invokeLoading(false);
        this.loading = false;
      }
    });
  }
}
