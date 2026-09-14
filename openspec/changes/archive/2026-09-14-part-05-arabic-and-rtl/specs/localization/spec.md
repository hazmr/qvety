## Purpose

Defines which language and direction the product shows, per practice and per user, and which text is translated versus kept exactly as entered.

## ADDED Requirements

### Requirement: Arabic by default, per-user override
The default language SHALL be Arabic (`ar-EG`). Each user SHALL have a stored locale that overrides the default and survives logout and refresh.

#### Scenario: New user
- **WHEN** a user is created with no locale
- **THEN** their locale is `ar-EG` and the UI opens in Arabic

#### Scenario: User switches to English
- **WHEN** the user toggles the language to `en` in the header
- **THEN** `PATCH /api/v1/me` stores `en-EG`, the UI switches without reload glitches, and a refresh keeps English

### Requirement: Direction follows language
When the language is Arabic the document direction SHALL be `rtl` and all components SHALL lay out right-to-left, including tables, forms, icons that indicate direction, and date pickers. When English, `ltr`.

#### Scenario: Table in Arabic
- **WHEN** a list page is shown in Arabic
- **THEN** columns start at the right and sort icons and pagination are mirrored

### Requirement: UI text comes from translation files, never from templates
Every user-visible UI string and every coded value label (roles, species, statuses) SHALL come from `web/src/assets/i18n/<lang>.json`. No hard-coded strings in templates. Every screen SHALL work in both `ar` and `en`.

#### Scenario: Grep for hard-coded text
- **WHEN** templates are scanned for literal user-facing text
- **THEN** none is found

#### Scenario: New screen recipe
- **WHEN** a new screen is added
- **THEN** its keys are added to both `ar.json` and `en.json` and the screen is checked in both directions before the part closes

### Requirement: Clinic-entered text is never translated
Client names, patient names, notes, clinical notes, service names, and any other text entered by the clinic SHALL be stored and shown exactly as entered, regardless of the UI language.

#### Scenario: English UI, Arabic client name
- **WHEN** a user in English views a client named `أحمد محمد`
- **THEN** the name is shown as `أحمد محمد`

### Requirement: Dates and numbers follow the user's locale
Dates, times, and numbers SHALL be formatted with the user's locale data.

#### Scenario: Header date
- **WHEN** the header shows today's date
- **THEN** it is formatted per `ar-EG` or `en-EG` according to the user's locale

### Requirement: Server messages follow the caller's language
Validation and domain error messages SHALL be resolved from the `Accept-Language` header, which the client sends from the user's locale.

#### Scenario: Validation error in Arabic
- **WHEN** a user in Arabic submits an invalid form
- **THEN** the backend error message is returned in Arabic and shown in Arabic
