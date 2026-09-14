## Purpose

Defines how the whole database is backed up, how long copies are kept, and how often a restore is proven to work.

## ADDED Requirements

### Requirement: Nightly database dump
The whole database SHALL be dumped nightly (03:00 Cairo time) in custom format and uploaded to the `qvety-backups` bucket in object storage. Dumps SHALL be kept 30 days.

#### Scenario: Nightly run
- **WHEN** the backup sidecar runs
- **THEN** a new dump object exists in the bucket and objects older than 30 days are removed

### Requirement: Restore is drilled and logged
A restore into a scratch database, followed by starting the application against it and logging in, SHALL be performed before the first paying clinic and at least once a quarter after. Each drill SHALL be logged with date and who did it.

#### Scenario: Drill log
- **WHEN** the drill log is read
- **THEN** the last drill date and person are present and within the last quarter
