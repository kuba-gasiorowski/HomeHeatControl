import {
  Directive,
  ElementRef,
  forwardRef,
  HostListener,
  Renderer2,
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import { Time } from 'src/app/models/api/heat-status';

@Directive({
  selector: '[appInputTime]',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => InputTimeDirective),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => InputTimeDirective),
      multi: true,
    },
  ],
})
export class InputTimeDirective implements ControlValueAccessor, Validator {
  @HostListener('input', ['$event.target.value'])
  onChange = (_: any) => {};

  @HostListener('blur', ['$event.target'])
  onTouched = (_: any) => {};

  constructor(private renderer: Renderer2, private elementRef: ElementRef) {}

  writeValue(obj: Time | null): void {
    const time = obj || new Time("");
    this.renderer.setProperty(
      this.elementRef.nativeElement,
      'value',
      time.toString()
    );
  }

  registerOnChange(fn: any): void {
    this.onChange = (value: string) => {
      fn(new Time(value));
    };
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (control.value === null) return null;
    if (!(control.value instanceof Time)) return { invalidType: true };
    const time = control.value as Time;
    let map: { [key: string]: any } = {};

    if (!this.validTimeElement(time.hour, 23)) {
      map['invalidHour'] = true;
    }
    if (!this.validTimeElement(time.minute, 59)) {
      map['invalidMinute'] = true;
    }
    if (!this.validTimeElement(time.second, 59)) {
      map['invalidSecond'] = true;
    }
    if (Object.keys(map).length > 0) {
      map['invalidTime'] = true;
      return map;
    }
    return null;
  }

  private validTimeElement(elem: any, max: number): boolean {
    if (
      elem == null ||
      isNaN(elem) ||
      typeof elem !== 'number' ||
      elem < 0 ||
      elem > max ||
      elem !== Math.round(elem)
    ) {
      return false;
    }
    return true;
  }
}
