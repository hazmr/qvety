## MODIFIED Requirements

### Requirement: Status flow
Status SHALL be one of `scheduled`, `checked_in`, `in_progress`, `completed`, `cancelled`, `no_show`. Allowed transitions: `scheduled → checked_in | cancelled | no_show`; `checked_in → in_progress | cancelled`; `in_progress → completed`. Any other transition SHALL be rejected. Cancelling SHALL keep the row.

Two of these transitions SHALL follow clinical work rather than a manual action: opening a visit for an appointment SHALL move that appointment to `in_progress`, and closing the visit SHALL move it to `completed`. Both SHALL go through the same allowed-transition check and SHALL be audited like any other change. Arrival, cancellation, and no-show have no clinical event behind them and SHALL stay manual. A manual move to `in_progress` or `completed` SHALL remain allowed, so the desk can correct a day the clinical screen did not finish.

#### Scenario: Allowed transition
- **WHEN** a `checked_in` appointment is moved to `in_progress`
- **THEN** the status changes and the change is audited

#### Scenario: Disallowed transition
- **WHEN** a `completed` appointment is moved to `scheduled`
- **THEN** the response is 409

#### Scenario: Visit opened
- **WHEN** a visit is created for a `checked_in` appointment
- **THEN** that appointment becomes `in_progress` without a separate request

#### Scenario: Visit closed
- **WHEN** the visit for an `in_progress` appointment is closed
- **THEN** that appointment becomes `completed` without a separate request

#### Scenario: Visit for an appointment that is not checked in
- **WHEN** a visit is created for an appointment that is `scheduled`
- **THEN** the visit is created and the appointment moves to `in_progress` through `checked_in`, so a patient seen without being checked in is still recorded as having arrived

#### Scenario: Desk closes the day by hand
- **WHEN** the desk moves an `in_progress` appointment to `completed` while its visit is still open
- **THEN** the status changes, the visit stays open, and the open-visit list still shows it
