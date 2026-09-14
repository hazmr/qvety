## ADDED Requirements

### Requirement: Automatic status transitions
In addition to manual changes, status SHALL move automatically: `trial → past_due` and `active → past_due` by the daily job on missed due dates; `past_due → suspended` by the job after grace; `trial | past_due | suspended → active` on a recorded payment. Every automatic change SHALL be a platform audit row and SHALL be reversible by a manual change.

#### Scenario: Job transition is audited
- **WHEN** the job moves a practice to `past_due`
- **THEN** the platform audit row names the job as the actor and the reason
