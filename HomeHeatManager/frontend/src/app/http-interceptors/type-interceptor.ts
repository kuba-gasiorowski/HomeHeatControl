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
import {Utils} from "../shared/utils";

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


  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      map((val: HttpEvent<any>) => {
        if (val instanceof HttpResponse) {
          val = val.clone();
          const body = val.body;
          Utils.convert(body);
        }
        return val;
      })
    );
  }
}
