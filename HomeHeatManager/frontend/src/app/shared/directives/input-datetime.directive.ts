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
import {formatDate} from "@angular/common";

@Directive({
  selector: '[appInputDateTime]',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => InputDateTimeDirective),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => InputDateTimeDirective),
      multi: true,
    },
  ],
})
export class InputDateTimeDirective implements ControlValueAccessor, Validator {
  @HostListener('input', ['$event.target.value'])
  onChange = (_: any) => {};

  @HostListener('blur', ['$event.target'])
  onTouched = (_: any) => {};

  constructor(private renderer: Renderer2, private elementRef: ElementRef) {}

  writeValue(obj: Date | string| null): void {
    if (obj === null || obj === "") {
      return;
    }
    const datetime = obj as Date;
    this.renderer.setProperty(
      this.elementRef.nativeElement,
      'value',
      formatDate(datetime, 'yyyy-MM-dd HH:mm:ss', 'en')
    );
  }

  registerOnChange(fn: any): void {
    this.onChange = (value: string) => {
      fn(new Date(Date.parse(value)));
    };
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (control.value === null) return null;
    if (!(control.value instanceof Date)) return { invalidType: true };
    const datetime = control.value as Date;
    if (isNaN(datetime.getTime())) return { invalidDate: true };
    return null;
  }
}
