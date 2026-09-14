import { HttpErrorResponse } from '@angular/common/http';
import { FormGroup } from '@angular/forms';

/** Mirrors com.qvety.common.ApiError; error bodies are not part of the OpenAPI schema. */
export interface ApiErrorDto {
  code: string;
  message: string;
  fields?: Record<string, string>;
}

/**
 * Backend errors arrive as {code, message, fields} already localized per Accept-Language.
 * Prefer that text over a client-side key; fall back when the body is not an ApiError.
 */
export function apiError(e: unknown): ApiErrorDto | null {
  if (e instanceof HttpErrorResponse && e.error && typeof e.error === 'object' && 'code' in e.error) {
    return e.error as ApiErrorDto;
  }
  return null;
}

export function apiMessage(e: unknown, fallback: string): string {
  return apiError(e)?.message ?? fallback;
}

/** Puts per-field backend messages on the matching controls so nzErrorTip can show them. */
export function applyFieldErrors(e: unknown, form: FormGroup): boolean {
  const fields = apiError(e)?.fields;
  if (!fields) return false;
  let applied = false;
  for (const [name, message] of Object.entries(fields)) {
    const control = form.get(name);
    if (control) {
      control.setErrors({ server: message });
      control.markAsTouched();
      applied = true;
    }
  }
  return applied;
}
