export class HeatingData {
  controllerStatus: boolean = false;
  lastStatusChangeTime: Date = new Date();
  lastMessageTime: Date = new Date();
  heatingPeriod: string = '';
  externalTemperature: number = 0;
  averageExternalTemperature: number = 0;
  ready: boolean = false;
}
