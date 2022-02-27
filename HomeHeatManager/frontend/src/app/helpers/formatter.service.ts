import { Inject, Injectable, LOCALE_ID } from '@angular/core';
import { formatNumber } from '@angular/common';

@Injectable({
  providedIn: 'root',
})
export class FormatterService {
  formatTemperature(temp?: number): string {
    if (temp) {
      return formatNumber(temp, this.locale, '1.2-2') + '\u00B0';
    } else {
      return '';
    }
  }

  constructor(@Inject(LOCALE_ID) public locale: string) {}
}
