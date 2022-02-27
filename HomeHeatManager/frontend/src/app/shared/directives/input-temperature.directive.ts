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

@Directive({
  selector: '[appInputTemperature]',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => InputTemperatureDirective),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: InputTemperatureDirective,
      multi: true,
    },
  ],
})
export class InputTemperatureDirective
  implements ControlValueAccessor, Validator
{
  @HostListener('input', ['$event.target.value'])
  onChange = (_: any) => {};

  @HostListener('blur', ['$event.target'])
  onTouched = (_: any) => {};

  constructor(private renderer: Renderer2, private elementRef: ElementRef) {}

  writeValue(obj: number): void {
    let data: number = obj || 0;
    this.renderer.setProperty(
      this.elementRef.nativeElement,
      'value',
      data.toFixed(2)
    );
  }

  registerOnChange(fn: any): void {
    this.onChange = (value: string) => {
      fn(value.trim() === '' ? NaN : Number(value));
    };
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (control.value === null) return null;
    if (isNaN(control.value)) {
      return { invalidTemperature: true };
    }
    return null;
  }
}
