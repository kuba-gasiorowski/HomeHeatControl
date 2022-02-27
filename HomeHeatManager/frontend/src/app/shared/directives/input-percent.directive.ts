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
  selector: '[appInputPercent]',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => InputPercentDirective),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: InputPercentDirective,
      multi: true,
    },
  ],
})
export class InputPercentDirective implements ControlValueAccessor, Validator {
  @HostListener('input', ['$event.target.value'])
  onChange = (_: any) => {};

  @HostListener('blur', ['$event.target'])
  onTouched = (_: any) => {};

  constructor(
    private elementRef: ElementRef<HTMLInputElement>,
    private renderer: Renderer2
  ) {}

  writeValue(obj: number): void {
    let data: number = obj || 0;
    this.renderer.setProperty(
      this.elementRef.nativeElement,
      'value',
      (data * 100).toFixed()
    );
  }

  registerOnChange(fn: any): void {
    this.onChange = (value: string) => {
      fn(value.trim() === '' ? NaN : Number(value) / 100);
    };
  }
  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (control.value === null) return null;
    if (isNaN(control.value)) return { invalidPercent: true };
    if (control.value > 1 || control.value < -1)
      return { invalidPercent: true };
    return null;
  }
}
