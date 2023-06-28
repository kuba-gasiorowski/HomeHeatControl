import { Component, OnInit } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ConfirmDialogService } from '../confirm-dialog/confirm-dialog.service';
import { CircuitConfig, Config, Time } from '../../models/api/heat-status';
import { BackendApiService } from '../../shared/backend-api.service';
import { ModalLoadingService } from '../modal-loading/modal-loading.service';

@Component({
  selector: 'app-general-config',
  templateUrl: './general-config.component.html',
  styleUrls: ['./general-config.component.css'],
})
export class GeneralConfigComponent implements OnInit {
  constructor(
    private backendApiService: BackendApiService,
    private confirmDialogService: ConfirmDialogService,
    private fb: FormBuilder,
    private loadingService: ModalLoadingService
  ) {
    this.configFormGroup = this.fb.group(
      {
        extMinTemp: [''],
        extMaxTemp: [''],
        extStartThreshold: [''],
        tempBaseLevel: [''],
        nightStartTime: [''],
        nightEndTime: [''],
        dayStartTime: [''],
        dayEndTime: [''],
        circuits: this.fb.array([]),
      },
      {
        validators: [this.extTempValidator, this.heatingPeriodValidator],
      }
    );
  }

  configFormGroup: FormGroup;
  public OperationType = OperationType;
  isActionExecuted = false;
  opType = OperationType.LOAD;
  opError: string = '';

  getCircuitsFormArray(): FormArray {
    return this.configFormGroup.get('circuits') as FormArray;
  }

  private addCircuitForm(): FormGroup {
    let circuitForm = this.fb.group(
      {
        index: [''],
        description: [''],
        active: [''],
        maxTemp: [''],
        tempBaseLevel: [''],
        nightAdjust: ['', [Validators.min(-0.3), Validators.max(0.3)]],
        dayAdjust: ['', [Validators.min(-0.3), Validators.max(0.3)]],
      } /*{
      validators: [this.circuitTempValidator]
    }*/
    );
    this.getCircuitsFormArray().push(circuitForm);
    return circuitForm;
  }

  /**
   * Retrieves the config data from the backend and
   * populates form using appropriate formatting if necessary.
   */
  getData(): void {
    this.opType = OperationType.LOAD;
    this.opError = '';
    this.loadingService.invokeLoading(true);
    this.isActionExecuted = true;
    this.backendApiService.getConfig().subscribe({
      next: (res) => {
        this.fillForm(res);
        this.loadingService.invokeLoading(false);
      },
      error: (err) => {
        this.loadingService.invokeLoading(false);
        this.opError = err;
      },
    });
  }

  fillForm(config: Config): void {
    this.configFormGroup.reset();
    this.getCircuitsFormArray().clear();
    this.isActionExecuted = false;
    if (config.circuits) {
      for (let i = 0; i < config.circuits.length; i++) {
        let circuitForm = this.addCircuitForm();
        circuitForm.patchValue({
          index: config.circuits[i].index,
          description: config.circuits[i].description,
          active: config.circuits[i].active,
          maxTemp: config.circuits[i].maxTemp,
          tempBaseLevel: config.circuits[i].tempBaseLevel,
          nightAdjust: config.circuits[i].nightAdjust,
          dayAdjust: config.circuits[i].dayAdjust,
        });
        circuitForm.addValidators([this.circuitTempValidator]);
      }
    }
    this.configFormGroup.patchValue({
      extMaxTemp: config.extMaxTemp,
      extMinTemp: config.extMinTemp,
      extStartThreshold: config.extStartThreshold,
      tempBaseLevel: config.tempBaseLevel,
      nightStartTime: config.nightStartTime,
      nightEndTime: config.nightEndTime,
      dayStartTime: config.dayStartTime,
      dayEndTime: config.dayEndTime,
    });
  }

  confirmSave(): void {
    this.confirmDialogService.doConfirm(
      'Do you want to apply the changes?',
      () => {
        this.saveChanges();
      },
      () => {
        this.cancelChanges();
      }
    );
  }

  refreshData(): void {
    this.confirmDialogService.doConfirm(
      'Do you want to discard all changes?',
      () => {
        this.getData();
      },
      () => {}
    );
  }

  saveChanges(): void {
    if (!this.configFormGroup.dirty) return;
    this.opType = OperationType.SAVE;
    this.opError = '';
    this.loadingService.invokeLoading(true);
    let config: Config = new Config();
    let circuitArr: CircuitConfig[] = [];
    Object.keys(this.configFormGroup.controls).forEach((key) => {
      if (key === 'circuits') {
        const circuitFormArray = <FormArray>this.configFormGroup.controls[key];
        for (let i = 0; i < circuitFormArray.controls.length; i++) {
          if (circuitFormArray.controls[i].dirty) {
            let circuit: CircuitConfig = new CircuitConfig();
            const circuitFormGroup = <FormGroup>circuitFormArray.controls[i];
            Object.keys(circuitFormGroup.controls).forEach((circuitKey) => {
              if (circuitFormGroup.controls[circuitKey].dirty) {
                circuit.index = i;
                circuit[circuitKey as keyof CircuitConfig] =
                  circuitFormGroup.controls[circuitKey].value;
              }
            });
            circuitArr.push(circuit);
          }
        }
      } else if (this.configFormGroup.controls[key].dirty) {
        config[key as keyof Config] = this.configFormGroup.controls[key].value;
      }
    });
    if (circuitArr.length > 0) {
      config.circuits = circuitArr;
    }
    this.backendApiService.updateConfig(config).subscribe({
      next: (res) => {
        this.fillForm(res);
        this.loadingService.invokeLoading(false);
      },
      error: (err) => {
        this.opError = err;
        this.loadingService.invokeLoading(false);
      },
    });
  }

  cancelChanges(): void {
    this.getData();
  }

  ngOnInit(): void {
    this.getData();
  }

  isInvalid(control: AbstractControl | null): boolean {
    if (control === null) return true;
    if ((control.touched || control.dirty) && control.invalid) return true;
    return false;
  }

  extTempValidator: ValidatorFn = (
    control: AbstractControl
  ): ValidationErrors | null => {
    const extMinTemp = control.get('extMinTemp');
    const extMaxTemp = control.get('extMaxTemp');
    if (
      extMaxTemp === null ||
      extMaxTemp.value === null ||
      extMinTemp === null ||
      extMinTemp.value === null ||
      isNaN(extMinTemp.value) ||
      isNaN(extMaxTemp.value)
    )
      return null;
    if (extMaxTemp.value <= extMinTemp.value) {
      extMinTemp.setErrors({ tempMismatch: true });
      extMinTemp.markAsTouched();
      extMaxTemp.setErrors({ tempMismatch: true });
      extMaxTemp.markAsTouched();
      return { error: 'extMinTemp >= extMaxTemp' };
    } else {
      extMinTemp.setErrors(null);
      extMaxTemp.setErrors(null);
    }
    return null;
  };

  heatingPeriodValidator: ValidatorFn = (
    control: AbstractControl
  ): ValidationErrors | null => {
    const nightStartTime = control.get('nightStartTime');
    const nightEndTime = control.get('nightEndTime');
    const dayStartTime = control.get('dayStartTime');
    const dayEndTime = control.get('dayEndTime');
    if (
      nightStartTime?.value === null ||
      Object.keys(nightStartTime?.value).length == 0 ||
      nightEndTime?.value === null ||
      Object.keys(nightEndTime?.value).length == 0 ||
      dayStartTime === null ||
      Object.keys(dayStartTime?.value).length == 0 ||
      dayEndTime === null ||
      Object.keys(dayEndTime?.value).length == 0
    )
      return null;
    if (
      nightStartTime?.value.isInvalid() ||
      nightEndTime?.value.isInvalid() ||
      dayStartTime?.value.isInvalid() ||
      dayEndTime?.value.isInvalid()
    )
      return null;
    let nightStartTimeTs = nightStartTime?.value.toTimestamp();
    let nightEndTimeTs = nightEndTime?.value.toTimestamp();
    let dayStartTimeTs = dayStartTime?.value.toTimestamp();
    let dayEndTimeTs = dayEndTime?.value.toTimestamp();
    if (nightStartTimeTs > nightEndTimeTs) {
      let shiftValue = 24 * 60 * 60 - nightStartTimeTs;
      nightStartTimeTs = 0;
      nightEndTimeTs += shiftValue;
      dayStartTimeTs += shiftValue;
      dayEndTimeTs += shiftValue;
    }
    let errorMap: { [key: string]: any } = {};
    if (dayStartTimeTs > dayEndTimeTs) {
      errorMap['invalidPeriod'] = true;
      errorMap['invalidDayPeriod'] = true;
      dayStartTime?.setErrors(errorMap);
      dayEndTime?.setErrors(errorMap);
      dayStartTime?.markAsTouched();
      dayEndTime?.markAsTouched();
    } else if (dayStartTimeTs < nightEndTimeTs || dayEndTimeTs > 24 * 60 * 60) {
      errorMap['invalidPeriod'] = true;
      errorMap['periodOverlap'] = true;
      dayStartTime?.setErrors(errorMap);
      dayEndTime?.setErrors(errorMap);
      nightStartTime?.setErrors(errorMap);
      nightEndTime?.setErrors(errorMap);
      dayStartTime?.markAsTouched();
      dayEndTime?.markAsTouched();
      nightStartTime?.markAsTouched();
      nightEndTime?.markAsTouched();
    } else {
      dayStartTime?.setErrors(null);
      dayEndTime?.setErrors(null);
      nightStartTime?.setErrors(null);
      nightEndTime?.setErrors(null);
    }
    if (Object.keys(errorMap).length > 0) {
      return errorMap;
    }
    return null;
  };

  circuitTempValidator(control: AbstractControl): ValidationErrors | null {
    const tempBaseLevel = control.get('tempBaseLevel');
    const maxTemp = control.get('maxTemp');
    if (
      tempBaseLevel === null ||
      tempBaseLevel.value === null ||
      isNaN(tempBaseLevel?.value) ||
      maxTemp === null ||
      maxTemp.value === null ||
      isNaN(maxTemp.value)
    )
      return null;
    let errorMap: { [key: string]: any } = {};
    if (tempBaseLevel.value >= maxTemp.value) {
      errorMap['invalidCircuitTemp'] = true;
      tempBaseLevel.setErrors(errorMap);
      maxTemp.setErrors(errorMap);
      tempBaseLevel.markAsTouched();
      maxTemp.markAsTouched();
      return errorMap;
    } else {
      tempBaseLevel.setErrors(null);
      maxTemp.setErrors(null);
    }
    return null;
  }
}

export enum OperationType {
  SAVE,
  LOAD,
}
