import { Component, Input, OnInit } from '@angular/core';
import { ModalLoadingService } from './modal-loading.service';

@Component({
  selector: 'app-modal-loading',
  templateUrl: './modal-loading.component.html',
  styleUrls: ['./modal-loading.component.css'],
})
export class ModalLoadingComponent implements OnInit {
  loading: boolean = false;

  constructor(private loadingService: ModalLoadingService) {}

  isLoading(): boolean {
    return this.loading;
  }

  ngOnInit(): void {
    this.loadingService.invokeLoadingEmitter.subscribe((l) => {
      this.loading = l;
      console.log('Loading: ', l);
    });
  }
}
