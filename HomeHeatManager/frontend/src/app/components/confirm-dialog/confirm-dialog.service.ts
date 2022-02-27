import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ConfirmDialogService {
  private subject = new Subject<any>();

  doConfirm(message: string, confirmFn: () => void, cancelFn: () => void): any {
    const that = this;
    this.subject.next({
      type: 'confirm',
      text: message,
      confirmFn(): any {
        that.subject.next(null);
        confirmFn();
      },
      cancelFn(): any {
        that.subject.next(null);
        cancelFn();
      },
    });
  }

  getMessage(): Observable<any> {
    return this.subject.asObservable();
  }
}
