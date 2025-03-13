import {Injectable, NgZone, OnDestroy, OnInit} from '@angular/core';
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
  tap, Observer,
} from 'rxjs';
import { environment } from 'src/environments/environment';
import {EventSource} from "eventsource";
  import {Utils} from "./utils";

@Injectable({
  providedIn: 'root',
})
export class BackendApiService {
  apiUrl: string;

  constructor(private zone: NgZone, private http: HttpClient) {
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
  statusEventSource: EventSource | undefined;

  getStatus(): Observable<HeatStatus> {
    return this.http
      .get<HeatStatus>(this.apiUrl + '/status')
      .pipe(map((res) => this.setStatusReady(res)))
      .pipe(map((res) => (this.lastStatus = res)))
      .pipe(tap((res) => this.heatStatus.next(res)));
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

  private streamStatus(): Observable<HeatStatus> {
    return new Observable((observer: Observer<HeatStatus>) => {
      let source = new EventSource(this.apiUrl + '/streamStatus',
        {
          fetch: (input , init) => {
            if (!init) {
              throw new Error('No init object');
            }
            const ri = input as RequestInfo;
            return fetch(ri, {
              ...init,
              headers: {
                ...init.headers,
                ...Utils.getAuthHeader()
              },
            });
          }
        }
      );
      this.statusEventSource = source;
      this.statusEventSource.addEventListener('HeatStatus', (event) => {
        this.zone.run(() => {
          observer.next(this.setStatusReady(Utils.convert(JSON.parse(event.data))));
        });
      });

    });

  }

  onStart(): void {
    this.stopStatusStream();
    this.subscription = this.streamStatus().subscribe({
      next: value => {
        this.lastStatus = value;
        this.heatStatus.next(value);},
    });
  }

  onDestroy(): void {
    this.stopStatusStream();
    this.subscription?.unsubscribe();
    this.lastStatus = new HeatStatus();
  }

  public stopStatusStream(): void {
    if (this.statusEventSource?.readyState !== EventSource.CLOSED) {
      this.statusEventSource?.close();
    }
  }
}
