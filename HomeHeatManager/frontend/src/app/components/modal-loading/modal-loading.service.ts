import { EventEmitter, Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ModalLoadingService {
  invokeLoadingEmitter = new EventEmitter<boolean>();

  invokeLoading(loading: boolean) {
    this.invokeLoadingEmitter.emit(loading);
  }
}
