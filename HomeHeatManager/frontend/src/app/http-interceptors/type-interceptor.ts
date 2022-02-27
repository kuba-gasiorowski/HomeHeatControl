import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { Time } from '../models/api/heat-status';

@Injectable()
export class TypeInterceptor implements HttpInterceptor {
  private _dateFormat =
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}?(\.\d*)((\+|\-)?\d{2}:\d{2}?)$/;

  isDateString(value: any): boolean {
    if (value == null || value == undefined) {
      return false;
    }
    if (typeof value === 'string') {
      return this._dateFormat.test(value);
    }
    return false;
  }

  isTimestampType(key: string): number {
    switch (key) {
      case 'lastStatusChangeTime':
        return 0.001;
      case 'lastMessageTime':
        return 1;
      default:
        return 0;
    }
  }

  isTimeType(key: string): boolean {
    switch (key) {
      case 'nightStartTime':
      case 'nightEndTime':
      case 'dayStartTime':
      case 'dayEndTime':
        return true;
      default:
        return false;
    }
  }

  convert(body: any) {
    if (body == null || body == undefined) {
      return body;
    }
    if (typeof body === 'object') {
      for (const key of Object.keys(body)) {
        const value = body[key];
        const factor = this.isTimestampType(key);
        if (factor > 0) {
          body[key] = new Date(parseInt(value, 10) * factor);
        } else if (this.isTimeType(key)) {
          body[key] = new Time(value);
        } else if (typeof value === 'object') {
          this.convert(value);
        }
      }
    }
    return body;
  }

  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      map((val: HttpEvent<any>) => {
        if (val instanceof HttpResponse) {
          val = val.clone();
          const body = val.body;
          this.convert(body);
        }
        return val;
      })
    );
  }
}
