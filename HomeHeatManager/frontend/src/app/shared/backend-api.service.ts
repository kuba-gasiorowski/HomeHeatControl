import { Injectable, OnDestroy, OnInit } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpHeaders,
} from '@angular/common/http';
import { CircuitConfig, CircuitMode, Config, HeatStatus } from '../models/api/heat-status';
import {
  map,
  Observable,
  Subscription,
  Subject,
  switchMap,
  timer,
  throwError,
  catchError,
  NEVER,
  of,
  interval,
  tap,
} from 'rxjs';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root',
})
export class BackendApiService {
  apiUrl: string;

  constructor(private http: HttpClient) {
    this.lastStatus = new HeatStatus();
    this.apiUrl = environment.apiUrl;
  }

  httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
    }),
  };

  heatStatus: Subject<HeatStatus> = new Subject<HeatStatus>();
  subscription: Subscription = Subscription.EMPTY;
  lastStatus: HeatStatus;

  getStatus(): Observable<HeatStatus> {
    return this.http
      .get<HeatStatus>(this.apiUrl + '/status')
      .pipe(map((res) => this.setStatusReady(res)))
      .pipe(map((res) => (this.lastStatus = res)));
    //return this.http.get<HeatStatus>("assets/status", this.httpOptions).pipe(map(res => this.setStatusReady(res)));
  }

  getConfig(): Observable<Config> {
    return this.http.get<Config>(this.apiUrl + '/config', this.httpOptions);
  }

  updateCircuit(index: number, active: CircuitMode): Observable<CircuitConfig> {
    return this.http
      .post<CircuitConfig>(
        this.apiUrl + '/config/circuit/' + index,
        { index: index, active: active } as CircuitConfig,
        this.httpOptions
      )
      .pipe(
        tap((circuit) => {
          this.lastStatus.circuitStatuses[index].circuitStatus = active;
        })
      );
  }

  updateConfig(config: Config): Observable<Config> {
    return this.http
      .post<Config>(this.apiUrl + '/config', config, this.httpOptions)
      .pipe(
        tap((updatedConfig) => {
          updatedConfig.circuits?.forEach(
            (value: CircuitConfig, index: number) => {
              this.lastStatus.circuitStatuses[index].circuitStatus =
                value.active as CircuitMode;
            }
          );
        })
      );
  }

  setStatusReady(heatStatusParam: HeatStatus): HeatStatus {
    heatStatusParam.ready = true;
    return heatStatusParam;
  }

  handleError(error: HttpErrorResponse) {
    if (error.status === 0) {
      console.error('An error occured: ', error.error);
    } else {
      console.error(
        `Backend returned code ${error.status}, body was: `,
        error.error
      );
    }
  }

  onStart(): void {
    this.subscription = timer(0, 60000)
      .pipe(
        switchMap(() =>
          this.getStatus().pipe(
            catchError((err) => {
              this.handleError(err);
              return NEVER;
            })
          )
        )
      )
      .subscribe((result) => this.heatStatus.next(result));
  }

  onDestroy(): void {
    this.subscription.unsubscribe();
    this.lastStatus = new HeatStatus();
  }
}
