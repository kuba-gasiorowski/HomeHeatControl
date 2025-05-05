import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { GeneralHeatingInfoComponent } from './components/general-heating-info/general-heating-info.component';
import { CircuitDataComponent } from './components/circuit-data/circuit-data.component';
import { httpInterceptorProviders } from './http-interceptors';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GeneralConfigComponent } from './components/general-config/general-config.component';
import { LoginComponent } from './components/login/login.component';
import { ConfirmDialogComponent } from './components/confirm-dialog/confirm-dialog.component';
import { InputTimeDirective } from './shared/directives/input-time.directive';
import { InputPercentDirective } from './shared/directives/input-percent.directive';
import { InputTemperatureDirective } from './shared/directives/input-temperature.directive';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ModalLoadingComponent } from './components/modal-loading/modal-loading.component';
import {InputDateTimeDirective} from "./shared/directives/input-datetime.directive";
import {DatePipe} from "@angular/common";

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    GeneralHeatingInfoComponent,
    CircuitDataComponent,
    GeneralConfigComponent,
    LoginComponent,
    ConfirmDialogComponent,
    InputDateTimeDirective,
    InputTimeDirective,
    InputPercentDirective,
    InputTemperatureDirective,
    ModalLoadingComponent,
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    AppRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    NgbModule,
  ],
  providers: [httpInterceptorProviders, DatePipe],
  bootstrap: [AppComponent],
})
export class AppModule {}
