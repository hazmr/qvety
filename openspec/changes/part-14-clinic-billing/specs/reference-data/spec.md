## ADDED Requirements

### Requirement: Appointment type default service
An appointment type MAY reference a default service; when set, invoice prefill SHALL use it so an exam prefills its consultation fee on the invoice.

#### Scenario: Exam type with default service
- **WHEN** an invoice is created from a visit whose appointment type has a default service
- **THEN** the draft's first line is that service at its current price
