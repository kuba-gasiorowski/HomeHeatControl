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

  isTimestampType(key: string): boolean {
    switch (key) {
      case 'lastStatusChangeTime':
      case 'lastMessageTime':
        return true;
      default:
        return false;
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
        if (this.isTimestampType(key)) {
          body[key] = new Date(parseInt(value, 10));
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
