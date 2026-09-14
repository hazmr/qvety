## ADDED Requirements

### Requirement: Practice logo
An `admin` SHALL upload a logo stored in object storage under `logo_object_key`; it SHALL print on invoices, prescriptions, and vaccination certificates.

#### Scenario: Logo on print
- **WHEN** a logo is set and a certificate is printed
- **THEN** the logo appears in the header
