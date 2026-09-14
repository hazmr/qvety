## ADDED Requirements

### Requirement: Visit close requires invoice or no-charge
Closing a visit SHALL require, in addition to the note state, either a non-void invoice linked to the visit or a `no_charge_reason`.

#### Scenario: Close without invoice
- **WHEN** close is called on a visit with a finalized note, no invoice, and no reason
- **THEN** the response is 409

#### Scenario: Close with no-charge
- **WHEN** close is called with a `no_charge_reason`
- **THEN** the visit closes and leaves the unbilled list
