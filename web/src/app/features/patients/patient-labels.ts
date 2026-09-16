import { TranslocoService } from '@jsverse/transloco';
import { PatientDto } from '../../api';

/** Enum values as the API spells them; labels come from i18n (`species.*`, `sex.*`, `severity.*`). */
export const SPECIES = ['dog', 'cat', 'bird', 'rabbit', 'rodent', 'reptile', 'horse', 'livestock', 'other'] as const;
export const SEXES = ['unknown', 'male', 'female', 'male_neutered', 'female_spayed'] as const;
export const SEVERITIES = ['mild', 'moderate', 'severe'] as const;

/** Age from the date of birth (years, or months under two), else the typed approximation, else nothing. */
export function ageLabel(t: TranslocoService, p: Pick<PatientDto, 'dateOfBirth' | 'ageApproximate'>, now = new Date()): string {
  if (p.dateOfBirth) {
    const dob = new Date(p.dateOfBirth);
    let months = (now.getFullYear() - dob.getFullYear()) * 12 + now.getMonth() - dob.getMonth();
    if (now.getDate() < dob.getDate()) months -= 1;
    if (months < 0) months = 0;
    return months < 24 ? t.translate('patients.ageMonths', { n: months }) : t.translate('patients.ageYears', { n: Math.floor(months / 12) });
  }
  return p.ageApproximate ?? '';
}

/** Today's date as the API and <input type="date"> expect it. */
export function isoDate(d = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** Local wall-clock value for <input type="datetime-local">. */
export function isoLocalDateTime(d = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${isoDate(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
