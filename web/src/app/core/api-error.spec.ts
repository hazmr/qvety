import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { apiMessage, applyFieldErrors } from './api-error';

/** Part 05, task 3.2: localized backend messages reach the screen instead of a client-side key. */
describe('api-error', () => {
  const arabic = new HttpErrorResponse({
    status: 400,
    error: { code: 'validation_failed', message: 'بعض الحقول غير صحيحة.', fields: { fullName: 'مطلوب.' } },
  });

  it('prefers the backend message', () => {
    expect(apiMessage(arabic, 'fallback')).toBe('بعض الحقول غير صحيحة.');
    expect(apiMessage(new Error('x'), 'fallback')).toBe('fallback');
  });

  it('puts field messages on the matching controls', () => {
    const form = new FormGroup({ fullName: new FormControl(''), phone: new FormControl('') });
    expect(applyFieldErrors(arabic, form)).toBe(true);
    expect(form.controls.fullName.errors).toEqual({ server: 'مطلوب.' });
    expect(form.controls.phone.errors).toBeNull();
  });
});
