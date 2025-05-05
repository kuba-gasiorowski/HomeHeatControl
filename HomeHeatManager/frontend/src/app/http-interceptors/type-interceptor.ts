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
import {DatePipe} from "@angular/common";

@Injectable()
export class TypeInterceptor implements HttpInterceptor {
  private _dateFormat =
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}?(\.\d*)((\+|\-)?\d{2}:\d{2}?)$/;


  constructor(private datePipe: DatePipe) {}

  isDateString(value: any): boolean {
    if (value == null || value == undefined) {
      return false;
    }
    if (typeof value === 'string') {
      return this._dateFormat.test(value);
    }
    return false;
  }


  private formatDates(body: any) {
    if (body && body.offHome) {
      body.offHome.forEach((offHome : any) => {
        if (offHome.decreaseFrom) {
          offHome.decreaseFrom = this.datePipe.transform(offHome.decreaseFrom, 'yyyy-MM-dd HH:mm:ss');
        }
        if (offHome.decreaseTo) {
          offHome.decreaseTo = this.datePipe.transform(offHome.decreaseTo, 'yyyy-MM-dd HH:mm:ss');
        }
      });
    }
  }


  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    const newBody = { ...req.body };
    this.formatDates(newBody);
    req = req.clone({ body: newBody });
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
