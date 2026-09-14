## ADDED Requirements

### Requirement: Patient photo
A patient MAY have a photo stored in object storage; only the object key SHALL be persisted; upload and display SHALL go through short-lived signed URLs after a tenant-scoped lookup.

#### Scenario: Upload photo
- **WHEN** `PUT /api/v1/patients/{id}/photo` is called
- **THEN** a signed upload URL is returned and, after upload, the key is stored and the photo shows on the patient header
