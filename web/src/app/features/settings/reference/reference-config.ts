import { inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AppointmentTypeDto, ReferenceApi, RoomDto, ServiceDto } from '../../../api';
import { FormField } from '../../../shared/form-page/form-page';
import { ListColumn } from '../../../shared/list-page/list-page';

/** Every reference row the screens care about; the three DTOs all satisfy it. */
export interface ReferenceRow {
  id: string;
  name: string;
  active: boolean;
  version: number;
}

/**
 * One config object per reference entity, the whole of what a reference screen needs. A fourth entity is
 * one more entry here plus its route: no new list, no new form, no new service (docs/domain/reference-data.md).
 */
export interface ReferenceConfig {
  /** Route segment under /settings, and the i18n key prefix: reference.<key>.title. */
  key: string;
  titleKey: string;
  addKey: string;
  columns: ListColumn<ReferenceRow>[];
  fields: FormField[];
  list(includeInactive: boolean): Observable<ReferenceRow[]>;
  get(id: string): Observable<ReferenceRow>;
  create(values: Record<string, string>): Observable<ReferenceRow>;
  update(id: string, version: number, values: Record<string, string>): Observable<ReferenceRow>;
  deactivate(id: string): Observable<ReferenceRow>;
  activate(id: string): Observable<ReferenceRow>;
}

/**
 * Built inside an injection context (a component field). The row type is fixed by the route, so the
 * columns take the shared {@link ReferenceRow} and narrow where they read an entity's own field.
 */
export function referenceConfigs(): Record<string, ReferenceConfig> {
  const api = inject(ReferenceApi);

  const rooms: ReferenceConfig = {
    key: 'rooms',
    titleKey: 'reference.rooms.title',
    addKey: 'reference.rooms.add',
    columns: [{ key: 'name', labelKey: 'reference.name', role: 'title' }],
    fields: [{ name: 'name', labelKey: 'reference.name', required: true, maxLength: 100 }],
    list: (includeInactive) => api.listRooms(includeInactive),
    get: (id) => api.getRoom(id),
    create: (v) => api.createRoom({ name: v['name'] }),
    update: (id, version, v) => api.updateRoom(id, version, { name: v['name'] }),
    deactivate: (id) => api.deactivateRoom(id),
    activate: (id) => api.activateRoom(id),
  };

  const appointmentTypes: ReferenceConfig = {
    key: 'appointment-types',
    titleKey: 'reference.appointmentTypes.title',
    addKey: 'reference.appointmentTypes.add',
    columns: [
      { key: 'name', labelKey: 'reference.name', role: 'title' },
      { key: 'durationMinutes', labelKey: 'reference.duration', role: 'secondary', value: (r) => `${(r as AppointmentTypeDto).durationMinutes}` },
      { key: 'color', labelKey: 'reference.color', role: 'hidden', ltr: true, mono: true },
    ],
    fields: [
      { name: 'name', labelKey: 'reference.name', required: true, maxLength: 100 },
      { name: 'durationMinutes', labelKey: 'reference.duration', type: 'number', required: true, hintKey: 'reference.durationHint' },
      { name: 'color', labelKey: 'reference.color', ltr: true, maxLength: 7, hintKey: 'reference.colorHint' },
    ],
    list: (includeInactive) => api.listAppointmentTypes(includeInactive),
    get: (id) => api.getAppointmentType(id),
    create: (v) => api.createAppointmentType(typeBody(v)),
    update: (id, version, v) => api.updateAppointmentType(id, version, typeBody(v)),
    deactivate: (id) => api.deactivateAppointmentType(id),
    activate: (id) => api.activateAppointmentType(id),
  };

  const services: ReferenceConfig = {
    key: 'services',
    titleKey: 'reference.services.title',
    addKey: 'reference.services.add',
    columns: [
      { key: 'name', labelKey: 'reference.name', role: 'title' },
      { key: 'price', labelKey: 'reference.price', role: 'secondary', ltr: true,
        value: (r) => `${(r as ServiceDto).price} ${(r as ServiceDto).currency}` },
    ],
    // No currency field: it is the practice currency and the server sets it.
    fields: [
      { name: 'name', labelKey: 'reference.name', required: true, maxLength: 100 },
      { name: 'price', labelKey: 'reference.price', type: 'number', required: true, ltr: true, hintKey: 'reference.priceHint' },
    ],
    list: (includeInactive) => api.listServices(includeInactive),
    get: (id) => api.getService(id),
    create: (v) => api.createService({ name: v['name'], price: Number(v['price']) }),
    update: (id, version, v) => api.updateService(id, version, { name: v['name'], price: Number(v['price']) }),
    deactivate: (id) => api.deactivateService(id),
    activate: (id) => api.activateService(id),
  };

  return { rooms, 'appointment-types': appointmentTypes, services };
}

/** An empty colour means "no colour"; the day view picks one. */
function typeBody(v: Record<string, string>) {
  return { name: v['name'], durationMinutes: Number(v['durationMinutes']), color: v['color'] || undefined };
}
