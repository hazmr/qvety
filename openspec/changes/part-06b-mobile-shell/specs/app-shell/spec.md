## Purpose

Defines the frame every screen lives in: one phone layout below 768 px and one desktop layout above, with navigation, title bar, action bar, target sizes, and direction rules that every later part inherits.

## ADDED Requirements

### Requirement: One phone layout below 768 px
The application SHALL render a single phone layout when the viewport is narrower than 768 px and the desktop layout otherwise. There SHALL be no separate tablet layout.

#### Scenario: Phone viewport
- **WHEN** the viewport is 390 px or 360 px wide
- **THEN** the phone layout is shown, nothing overflows horizontally, and the content is readable without zooming

#### Scenario: Desktop viewport
- **WHEN** the viewport is 1280 px wide
- **THEN** the desktop layout is shown: a 32 px header strip and a 196 px sidebar holding the navigation, collapsible to the mark

### Requirement: Phone skeleton
On phone every screen SHALL be a column of: a 32 px brand strip (primary fill, white mark, clinic name, user initials); a 48 px title bar with the page name and at most one action; an optional filter or tab row with 44 px cells; exactly one scrolling region; and either a 60 px bottom navigation bar or a 48 px action bar, never both. Brand strip and title bar SHALL NOT scroll. The bottom element SHALL respect `env(safe-area-inset-bottom)`.

#### Scenario: Top-level screen
- **WHEN** a top-level destination (home, clients, users) is open on phone
- **THEN** the bottom navigation is shown and no action bar

#### Scenario: Detail screen
- **WHEN** a detail screen (client record) is open on phone
- **THEN** the title bar shows a back chevron and the screen's actions sit in a 48 px action bar at the bottom, with no bottom navigation

### Requirement: Lists are stacked rows on phone
On phone, lists SHALL NOT use a data table. Each row SHALL stack its fields (title at 15 px / 600, secondary line at 13 px muted, optional trailing tag), have a minimum height of 44 px, and be separated by 1 px rules. Search and paging SHALL keep working.

#### Scenario: Clients list on phone
- **WHEN** the clients list is opened at 390 px
- **THEN** each client is one tappable row with the name, then phone and email on a second line, and no table header

### Requirement: Touch targets and input sizes
On phone, text inputs SHALL be 16 px type in a 52 px box (so iOS does not zoom), inline buttons SHALL be at least 44 px tall and action-bar buttons 48 px, page and card padding SHALL be 14 px. Desktop sizes SHALL be unchanged.

#### Scenario: Form on phone
- **WHEN** the client form is opened at 390 px
- **THEN** every input is 52 px tall with 16 px text and the save button is at least 44 px tall

### Requirement: Login on phone is a bottom sheet
On phone the login screen SHALL show the stacked lockup on a `teal-soft` ground with the form as a surface sheet anchored to the bottom (2 px ink top rule), holding two 52 px inputs, a 52 px primary button, and a 44 px footer row with the language switch. On desktop the centred card SHALL remain.

#### Scenario: Login at 390 px
- **WHEN** the login page is opened at 390 px in Arabic
- **THEN** the sheet sits at the bottom, inputs are LTR, and the language switch is in the footer row

### Requirement: Direction rules hold on phone
Mirroring SHALL follow `dir` on the document root: navigation order, row internals, tab strips, sheets, and the back chevron mirror; the mark, object icons, and Latin-content fields (phone, email, ids) do not.

#### Scenario: Back chevron in Arabic
- **WHEN** a detail screen is open at 390 px in Arabic
- **THEN** the back chevron points right and sits at the start (right) edge of the title bar

### Requirement: Every screen is checked on phone before a part closes
Each part's close-out SHALL include a check at 390 px and 360 px in both languages, in addition to desktop.

#### Scenario: Part close-out
- **WHEN** a part's tasks are ticked complete
- **THEN** its screens have been checked at 390 px, 360 px, and 1280 px in `ar` and `en`
