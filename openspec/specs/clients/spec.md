# clients Specification

## Purpose
Defines the client: the person who owns the animal and pays. Covers required data, archiving instead of deletion, duplicate handling, and data deliberately not collected.

## Requirements

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

### Requirement: National ID is not stored
The system SHALL NOT have a column or field for the national ID. It is personal data under Egypt's Personal Data Protection Law (151/2020) and no pilot feature uses it. Adding it requires a written reason from a clinic and a new part.

#### Scenario: Schema check
- **WHEN** the `clients` table is inspected
- **THEN** no national id column exists

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

### Requirement: Duplicate warning does not block
On create, the system SHALL return candidate duplicates (same phone in either phone column, or same normalized name) in a `warnings` field. Creation SHALL still succeed.

#### Scenario: Same phone
- **WHEN** a client is created with a phone that another client already has
- **THEN** the response is 201 and `warnings` lists the existing client

### Requirement: Roles for client data
`front_desk` and `admin` SHALL create and update clients. Every role SHALL read.

#### Scenario: Technician creates a client
- **WHEN** a `technician` calls `POST /api/v1/clients`
- **THEN** the response is 403

### Requirement: Lists are paged and capped
List endpoints SHALL accept `page`, `size`, `sort`, and SHALL cap `size` at 100 regardless of the request.

#### Scenario: Oversized page
- **WHEN** `GET /api/v1/clients?size=1000` is called
- **THEN** at most 100 rows are returned

### Requirement: One error shape
Validation and domain errors SHALL be returned as `{code, message, fields: {name: message}}`, localized per the caller's language.

#### Scenario: Invalid body
- **WHEN** a create request fails validation
- **THEN** the response is 400 with `code`, a localized `message`, and `fields` naming each invalid field

### Requirement: Concurrent edits are detected
Client rows SHALL carry a version; a stale update SHALL be rejected with 409.

#### Scenario: Two tabs
- **WHEN** the same client is edited from two sessions and the second saves with the older version
- **THEN** the second save returns 409 and no data is silently overwritten

### Requirement: Cross-tenant read is not found
See `tenant-boundary`. A client of another practice SHALL return 404.

#### Scenario: Other practice's client
- **WHEN** practice A requests practice B's client by id
- **THEN** the response is 404

### Requirement: Client search by name or phone
`GET /api/v1/clients?q=` SHALL: if `q` parses as a phone, match `phone_e164` or `phone_secondary_e164`; otherwise fold `q` and match `full_name_normalized` by substring or by trigram similarity above 0.3, ordered by similarity.

#### Scenario: Fuzzy name
- **WHEN** the search is a close misspelling of a stored name
- **THEN** the client appears, ranked by similarity

### Requirement: Duplicate warning uses normalized values
The create-time duplicate warning SHALL compare `phone_e164` (either column) and `full_name_normalized`.

#### Scenario: Variant spelling duplicate
- **WHEN** a client `احمد محمد` is created and `أحمد محمد` already exists
- **THEN** `warnings` lists the existing client and creation still succeeds

### Requirement: Normalized columns are maintained on every save
The service SHALL set `full_name_normalized`, `phone_e164`, and `phone_secondary_e164` on every create and update.

#### Scenario: Rename
- **WHEN** a client's `full_name` is updated
- **THEN** `full_name_normalized` reflects the new name
