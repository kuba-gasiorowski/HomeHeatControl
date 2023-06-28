import { CircuitMode } from "./api/heat-status";

export class CircuitData {
  index: number = -1;
  name: string = '';
  temperature: number = 0;
  heating: boolean = false;
  active: CircuitMode = CircuitMode.OFF;
}
