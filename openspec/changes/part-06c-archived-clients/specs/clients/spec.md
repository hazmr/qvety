## MODIFIED Requirements

### Requirement: Client identity fields
A client SHALL have a `full_name` (free text, typed as the person says it; no first/last split) and a `phone` that parses as an Egyptian number. Optional: `preferred_name`, `phone_secondary`, `email`, `address`, `notes`, `preferred_locale`. The clinic reaches owners by phone and WhatsApp; a client without a phone cannot be recalled, so email alone is not enough.

#### Scenario: Name only
- **WHEN** a client is created with a name and no phone
- **THEN** the request is rejected with a validation error on the `phone` field

#### Scenario: Name and email only
- **WHEN** a client is created with a name and an email but no phone
- **THEN** the request is rejected with a validation error on the `phone` field

#### Scenario: Arabic name chain
- **WHEN** a client is created with `full_name` `أحمد محمد علي حسن`
- **THEN** it is stored and shown exactly as typed

### Requirement: Clients are archived, never deleted
A client SHALL be archivable via `POST /api/v1/clients/{id}/archive`, setting `archived_at`, and restorable via `POST /api/v1/clients/{id}/unarchive`, clearing it. There SHALL be no delete. Archived clients SHALL be excluded from default lists, from search, and from recalls, but remain readable with their history. `GET /api/v1/clients?includeArchived=true` SHALL include archived clients in the list and in search, each marked with `archivedAt`. The clients list SHALL offer a "show archived" switch, off by default, and the client detail SHALL offer Unarchive in place of Archive while the client is archived. Both endpoints SHALL follow the same roles as create and update. Unarchive on another practice's client SHALL return 404.

#### Scenario: Archive
- **WHEN** a client with patients and records is archived
- **THEN** the row and all related rows remain and the client no longer appears in the default list or in search

#### Scenario: Show archived
- **WHEN** `GET /api/v1/clients?includeArchived=true&q=<the archived client's phone>` is called
- **THEN** the archived client is returned with `archivedAt` set

#### Scenario: Unarchive
- **WHEN** `POST /api/v1/clients/{id}/unarchive` is called on an archived client
- **THEN** `archivedAt` is null, the client appears in the default list again, and edits are accepted

#### Scenario: Unarchive across practices
- **WHEN** practice A calls unarchive on practice B's archived client
- **THEN** the response is 404 and the client stays archived
