import { expect, test, type Page } from '@playwright/test';

/**
 * The definition of "checked on phone" (docs/progress.md, part 06b). Every screen, at every viewport, in
 * both languages, must satisfy the layout skeleton in docs/design/MOBILE.md:
 * - no horizontal overflow;
 * - phone: bottom nav or action bar, never both; no nz-table, stacked rows instead; desktop: the sidebar;
 * - inputs at least 16 px text and 44 px targets on phone (the one title-bar action is the documented
 *   36 px compact button inside the 48 px bar, MOBILE.md, so it is left out of that check);
 * - zero console errors.
 * Logs in as the seeded admin (R__020_dev_users.sql) and forces the locale on the user row first, so the
 * run does not depend on what the last person left there.
 */

const API = process.env['E2E_API_URL'] ?? 'http://localhost:8080';
const ADMIN = { identifier: 'admin@clinic.example.com', password: 'password123' };
const CLIENT = '00000000-0000-7000-8000-000000000301';   // seeded client with patients
const PATIENT = '00000000-0000-7000-8000-000000000401';
const DESK_USER = '00000000-0000-7000-8000-000000000104';

type Locale = 'ar' | 'en';

const SCREENS: { name: string; path: string; ready: string }[] = [
  { name: 'home', path: '/', ready: '.home__practice' },
  { name: 'clients', path: '/clients', ready: '.title-bar' },
  { name: 'client-detail', path: `/clients/${CLIENT}`, ready: '.summary' },
  { name: 'client-form', path: '/clients/new', ready: '#fullName' },
  { name: 'patient-detail', path: `/clients/${CLIENT}/patients/${PATIENT}`, ready: '.summary' },
  { name: 'patient-form', path: `/clients/${CLIENT}/patients/new`, ready: '#name' },
  { name: 'users', path: '/settings/users', ready: '.title-bar' },
  { name: 'user-form', path: `/settings/users/${DESK_USER}`, ready: 'input[formcontrolname=fullName]' },
];

for (const locale of ['ar', 'en'] as Locale[]) {
  test(`walkthrough ${locale}`, async ({ page, request, viewport }) => {
    const phone = (viewport?.width ?? 1280) < 768;
    const errors: string[] = [];
    page.on('pageerror', (e) => errors.push(e.message));
    page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text().slice(0, 300)); });

    await setUserLocale(request, locale);
    await page.goto('/login');
    await page.evaluate((l) => localStorage.setItem('qvety.locale', l), locale === 'ar' ? 'ar-EG' : 'en-EG');
    await page.reload();
    await checkScreen(page, 'login', phone, locale);

    await page.fill('#identifier', ADMIN.identifier);
    await page.fill('#password', ADMIN.password);
    await page.click('button[type=submit]');
    await page.waitForURL((u) => !u.pathname.endsWith('/login'));
    await page.waitForLoadState('networkidle');   // fonts finish before the next navigation aborts them

    for (const s of SCREENS) {
      await page.goto(s.path);
      await page.waitForSelector(s.ready);
      await page.waitForLoadState('networkidle');
      await checkScreen(page, s.name, phone, locale);
    }
    if (phone) {
      await page.goto('/');
      await page.waitForSelector('.home__practice');
      await page.locator('.bottom-nav__cell').last().click();   // the Me sheet
      await page.waitForSelector('.me__btn');
      await checkScreen(page, 'me-sheet', phone, locale);
    }
    expect(errors, 'console errors').toEqual([]);
  });
}

/** PATCH /me so the login response carries the locale under test; the shell applies it on login. */
async function setUserLocale(request: Parameters<Parameters<typeof test>[1]>[0]['request'], locale: Locale) {
  const login = await request.post(`${API}/api/v1/auth/login`, { data: ADMIN });
  expect(login.ok(), 'seeded admin login').toBeTruthy();
  const token = (await login.json()).token as string;
  const me = await request.patch(`${API}/api/v1/me`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { locale: locale === 'ar' ? 'ar-EG' : 'en-EG' },
  });
  expect(me.ok(), 'set locale').toBeTruthy();
}

async function checkScreen(page: Page, name: string, phone: boolean, locale: Locale) {
  const r = await page.evaluate(() => {
    const visible = (e: Element) => (e as HTMLElement).offsetParent !== null;
    const heights = (sel: string) => [...document.querySelectorAll(sel)].filter(visible).map((e) => e.getBoundingClientRect().height);
    const fontSizes = (sel: string) => [...document.querySelectorAll(sel)].filter(visible).map((e) => parseFloat(getComputedStyle(e).fontSize));
    return {
      overflow: document.documentElement.scrollWidth > window.innerWidth,
      dir: document.documentElement.dir,
      nav: !!document.querySelector('.bottom-nav'),
      bar: !!document.querySelector('.action-bar--phone'),
      sidebar: !!document.querySelector('.sidebar'),
      table: !!document.querySelector('nz-table table'),
      minInputHeight: Math.min(...heights('input.ant-input, .ant-select-selector'), 999),
      minInputFont: Math.min(...fontSizes('input.ant-input'), 999),
      minButton: Math.min(...heights('button.ant-btn:not(.title-bar__action), a.ant-btn:not(.title-bar__action), .bottom-nav__cell'), 999),
    };
  });
  const tag = `${name} ${locale}`;
  expect(r.dir, `${tag}: dir`).toBe(locale === 'ar' ? 'rtl' : 'ltr');
  expect(r.overflow, `${tag}: horizontal overflow`).toBe(false);
  if (phone) {
    expect(r.sidebar, `${tag}: sidebar on phone`).toBe(false);
    expect(r.table, `${tag}: nz-table on phone`).toBe(false);
    if (name !== 'login') {
      expect(r.nav !== r.bar, `${tag}: exactly one of bottom nav / action bar (nav=${r.nav} bar=${r.bar})`).toBe(true);
    }
    expect(r.minInputFont, `${tag}: input font size`).toBeGreaterThanOrEqual(16);
    expect(r.minInputHeight, `${tag}: input height`).toBeGreaterThanOrEqual(44);
    expect(r.minButton, `${tag}: tap target height`).toBeGreaterThanOrEqual(44);
  } else if (name !== 'login') {
    expect(r.sidebar, `${tag}: sidebar on desktop`).toBe(true);
    expect(r.nav, `${tag}: bottom nav on desktop`).toBe(false);
  }
}
