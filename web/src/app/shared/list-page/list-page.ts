import { Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzTableModule, NzTableQueryParams } from 'ng-zorro-antd/table';
import { Observable, Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ViewportService } from '../../core/viewport.service';
import { TitleBar } from '../../layout/title-bar/title-bar';

/**
 * One column of a generic list. `ltr` marks Latin values (phones, emails, ids) that stay LTR inside Arabic.
 * `role` says where the value goes in the phone row: the title line, the secondary line, or a trailing tag;
 * `hidden` keeps a column desktop-only.
 */
export interface ListColumn<T> {
  key: string;
  labelKey: string;
  sortable?: boolean;
  ltr?: boolean;
  mono?: boolean;
  role?: 'title' | 'secondary' | 'tag' | 'hidden';
  value?: (row: T) => unknown;
}

/** Server-side page shape (Spring PagedModel). */
export interface PageResult<T> {
  content?: T[];
  page?: { size?: number; number?: number; totalElements?: number; totalPages?: number };
}

export interface ListQuery {
  q: string;
  page: number;   // zero-based, as Spring expects
  size: number;
  sort?: string;  // "field,asc"
}

/**
 * Generic NG-ZORRO table with server-side paging, sort, and a debounced search box. The feature supplies
 * columns, a loader, and where a row click goes. Part 06 clients is the first user; reference data (part 09)
 * reuses it with only a config object. A feature may project one extra control (`list-extra` beside the
 * desktop search box, `list-extra-phone` under the phone one) and call `reload()` when it changes.
 */
@Component({
  selector: 'app-list-page',
  imports: [FormsModule, NzTableModule, NzInputModule, NzButtonModule, TranslocoPipe, TitleBar],
  templateUrl: './list-page.html',
})
export class ListPage<T extends { id: string }> {
  private readonly router = inject(Router);
  readonly viewport = inject(ViewportService);

  readonly titleKey = input.required<string>();
  readonly columns = input.required<ListColumn<T>[]>();
  readonly loader = input.required<(query: ListQuery) => Observable<PageResult<T>>>();
  readonly rowLink = input<(row: T) => unknown[]>();
  readonly createLink = input<unknown[]>();
  readonly createKey = input<string>('shared.add');
  readonly searchable = input(true);
  readonly pageSize = input(20);
  readonly rowClicked = output<T>();

  readonly rows = signal<T[]>([]);
  readonly total = signal(0);
  readonly loading = signal(true);
  readonly pageIndex = signal(1);   // NG-ZORRO is one-based
  readonly sort = signal<string | undefined>(undefined);
  readonly q = signal('');
  readonly trackBy = computed(() => (_: number, row: T) => row.id);
  /** Phone row parts, derived from column roles (first column is the title when none is marked). */
  readonly titleCol = computed(() => this.columns().find((c) => c.role === 'title') ?? this.columns()[0]);
  readonly secondaryCols = computed(() => this.columns().filter((c) => c.role === 'secondary'));
  readonly tagCol = computed(() => this.columns().find((c) => c.role === 'tag'));
  readonly hasMore = computed(() => this.rows().length < this.total());

  private readonly search$ = new Subject<string>();

  constructor() {
    this.search$.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed()).subscribe((q) => {
      this.q.set(q);
      this.pageIndex.set(1);
      this.load();
    });
    // reload when the loader input changes; untracked so the signals load() reads do not re-trigger it
    effect(() => { this.loader(); untracked(() => this.load()); });
  }

  /** First page again with the current search and sort; for a projected filter control. */
  reload(): void {
    this.pageIndex.set(1);
    this.load();
  }

  onSearch(value: string): void {
    this.search$.next(value.trim());
  }

  /**
   * NG-ZORRO emits on page and sort changes; we own the page index so search can reset it. A column only
   * appears in `params.sort` when it has `nzSortFn`; `true` there means "the server sorts" and keeps the
   * table from sorting the page client-side.
   */
  onQuery(params: NzTableQueryParams): void {
    const active = params.sort.find((s) => s.value);
    const sort = active ? `${active.key},${active.value === 'descend' ? 'desc' : 'asc'}` : undefined;
    const changed = params.pageIndex !== this.pageIndex() || sort !== this.sort();
    this.pageIndex.set(params.pageIndex);
    this.sort.set(sort);
    if (changed) this.load();
  }

  open(row: T): void {
    this.rowClicked.emit(row);
    const link = this.rowLink();
    if (link) this.router.navigate(link(row));
  }

  cell(col: ListColumn<T>, row: T): unknown {
    return col.value ? col.value(row) : (row as Record<string, unknown>)[col.key];
  }

  /** Phone paging: append the next page instead of replacing the list. */
  loadMore(): void {
    this.pageIndex.update((i) => i + 1);
    this.load(true);
  }

  load(append = false): void {
    this.loading.set(true);
    this.loader()({ q: this.q(), page: this.pageIndex() - 1, size: this.pageSize(), sort: this.sort() }).subscribe({
      next: (p) => {
        const content = p.content ?? [];
        this.rows.set(append ? [...this.rows(), ...content] : content);
        this.total.set(p.page?.totalElements ?? 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  /** Secondary line parts; Latin values keep their own LTR span so a phone reads +20... inside Arabic. */
  secondaryParts(row: T): { key: string; value: unknown; ltr?: boolean; mono?: boolean }[] {
    return this.secondaryCols()
      .map((c) => ({ key: c.key, value: this.cell(c, row), ltr: c.ltr, mono: c.mono }))
      .filter((p) => p.value !== null && p.value !== undefined && p.value !== '');
  }
}
