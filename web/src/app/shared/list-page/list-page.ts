import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzTableModule, NzTableQueryParams } from 'ng-zorro-antd/table';
import { Observable, Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

/** One column of a generic list. `ltr` marks Latin values (phones, emails, ids) that stay LTR inside Arabic. */
export interface ListColumn<T> {
  key: string;
  labelKey: string;
  sortable?: boolean;
  ltr?: boolean;
  mono?: boolean;
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
 * reuses it with only a config object.
 */
@Component({
  selector: 'app-list-page',
  imports: [FormsModule, RouterLink, NzTableModule, NzInputModule, NzButtonModule, TranslocoPipe],
  templateUrl: './list-page.html',
})
export class ListPage<T extends { id: string }> {
  private readonly router = inject(Router);

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

  private readonly search$ = new Subject<string>();

  constructor() {
    this.search$.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed()).subscribe((q) => {
      this.q.set(q);
      this.pageIndex.set(1);
      this.load();
    });
    effect(() => { this.loader(); this.load(); });
  }

  onSearch(value: string): void {
    this.search$.next(value.trim());
  }

  /** NG-ZORRO emits on page and sort changes; we own the page index so search can reset it. */
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

  load(): void {
    this.loading.set(true);
    this.loader()({ q: this.q(), page: this.pageIndex() - 1, size: this.pageSize(), sort: this.sort() }).subscribe({
      next: (p) => {
        this.rows.set(p.content ?? []);
        this.total.set(p.page?.totalElements ?? 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
