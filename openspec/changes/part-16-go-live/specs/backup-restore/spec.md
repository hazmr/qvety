## ADDED Requirements

### Requirement: Off-site copy
The nightly dump SHALL also be mirrored to a bucket at a second provider. A restore drill from the off-site copy SHALL be done before the first clinic goes live and logged.

#### Scenario: Off-site drill
- **WHEN** the drill log is read before go-live
- **THEN** it contains a restore from the second provider's copy with date and person
