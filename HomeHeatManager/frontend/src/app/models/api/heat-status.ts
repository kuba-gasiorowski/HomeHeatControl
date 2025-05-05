export class HeatStatus {
  controllerStatus: boolean = false;
  lastStatusChangeTime: Date = new Date();
  lastMessageTime: Date = new Date();
  heatingPeriod: HeatingPeriod = HeatingPeriod.NO_HEATING;
  externalTemperature: number = 0;
  avgExternalTemperature: number = 0;

  circuitStatuses: CircuitStatus[] = [];

  ready: boolean = false;
}

export enum HeatingPeriod {
  NIGHT,
  DAY,
  NO_HEATING,
}

export enum CircuitMode {
  OFF = "OFF",
  NIGHT = "NIGHT",
  DAY = "DAY",
  ALL = "ALL",
}

export class CircuitStatus {
  circuitIndex: number = -1;
  circuitStatus: CircuitMode = CircuitMode.OFF;
  heatingOn: boolean = false;
  circuitName: string = '';
  circuitTemperature: number = 0;
}

export class Config {
  extMinTemp?: number;
  extMaxTemp?: number;
  extStartThreshold?: number;
  tempBaseLevel?: number;
  nightStartTime?: Time;
  nightEndTime?: Time;
  dayStartTime?: Time;
  dayEndTime?: Time;
  offHome?: OffHomeElement[];
  circuits?: CircuitConfig[];
}

export class OffHomeElement {
  decreaseFrom?: Date;
  decreaseTo?: Date;
  decreaseTemp?: number;
}

export class Time {
  hour: number = 0;
  minute: number = 0;
  second: number = 0;
  nano: number = 0;

  constructor(data: string) {
    let time = data.split(':').map((item) => {
      return parseInt(item, 10);
    });
    this.hour = time[0];
    this.minute = time[1];
    if (time.length > 2) this.second = time[2];
    if (time.length > 3) this.nano = time[3];
  }

  public toString(): string {
    return (
      this.hour?.toFixed().padStart(2, '0') +
      ':' +
      this.minute?.toFixed().padStart(2, '0') +
      ':' +
      this.second?.toFixed().padStart(2, '0')
    );
  }

  public toJSON(): number[] {
    let arr = [this.hour, this.minute];
    if (this.second != 0 || this.nano != 0) arr.push(this.second);
    if (this.nano != 0) arr.push(this.nano);
    return arr;
  }

  isInvalid(): boolean {
    if (
      isNaN(this.hour) ||
      isNaN(this.minute) ||
      isNaN(this.second) ||
      isNaN(this.nano) ||
      this.hour < 0 ||
      this.hour > 23 ||
      this.hour != Math.floor(this.hour) ||
      this.minute < 0 ||
      this.minute > 59 ||
      this.minute != Math.floor(this.minute) ||
      this.second < 0 ||
      this.second > 59 ||
      this.second != Math.floor(this.second) ||
      this.nano < 0 ||
      this.nano > 999999999 ||
      this.nano != Math.floor(this.nano)
    )
      return true;
    return false;
  }

  toTimestamp(): number {
    return (
      this.hour * 60 * 60 + this.minute * 60 + this.second + this.nano * 10e-9
    );
  }
}

export class CircuitConfig {
  index?: number;
  description?: string;
  active?: CircuitMode;
  maxTemp?: number;
  tempBaseLevel?: number;
  nightAdjust?: number;
  dayAdjust?: number;
  heatCharacteristics?: HeatCharacteristics[];
}

export class HeatCharacteristics {
  tempMax?: number;
  heatFactor?: number;
}
