# FairShare Security Mitigation Implementation Plan

## 1. Objective
Harden the Group Finance Tracker against spoofing, tampering, repudiation, information disclosure, denial of service, and privilege escalation, while preserving current product behavior.

## 2. Scope
- Android app (Java/XML)
- Firebase Auth
- Firestore data model and Security Rules
- Notification and settlement flows
- Deep-link entry points
- Operational monitoring and incident readiness

## 3. Guiding Principles
- Server-side authorization is the source of truth.
- Client-side checks are UX only, never security controls.
- Financial records are append-only where feasible.
- Least privilege for every write path.
- Roll out in phases with measurable gates.

## 4. Priority Risk Mapping
1. Tampering / Elevation of Privilege on group and settlement writes (Critical)
2. Spoofing and notification forgery/spam (High)
3. Information disclosure due to rule/schema drift (High)
4. Repudiation due to mutable-only event records (Medium)
5. Notification cleanup and write abuse creating DoS pressure (Medium)

## 5. Target Architecture (Security)
1. Firestore Rules strictly enforce membership and role checks based on actual document schema.
2. Sensitive writes move to trusted backend functions:
   - create nudge
   - confirm settlement
   - settle debt
   - group status transitions
3. Audit events stored in append-only collection with server timestamp and actor UID.
4. Android client becomes a requester, not authority, for financial state transitions.
5. If Blaze is unavailable, automated nudge and settlement confirmation must remain disabled rather than falling back to insecure client writes.

## 6. Phased Implementation Plan

## Phase 0: Preparation (1-2 days)
### Tasks
1. Freeze schema contract for `groups`, `group_expenses`, `notifications`, and new `audit_events`.
2. Create migration checklist and rollback strategy.
3. Capture baseline metrics:
   - Firestore read/write volume
   - notification create rate
   - permission-denied rate

### Deliverables
- Security schema contract doc
- Rollback runbook
- Baseline dashboard snapshot

### Exit Criteria
- All field definitions agreed and versioned.

## Phase 1: Firestore Rules Hardening (2-3 days)
### Tasks
1. Replace map-style membership checks with schema-accurate list checks (or migrate schema to maps and update app consistently).
2. Split permissions by operation:
   - `read`
   - `create`
   - `update`
   - `delete`
3. Enforce invariants in rules:
   - only creator can change group status
   - only involved members can read group expenses
   - only valid actor can mark a settlement for debtor
   - only recipient can mark `notifications.isRead`
4. Deny client writes for sensitive fields managed by backend:
   - notification sender identity fields
   - settlement confirmation fields if backend-owned

### Primary Files
- `firestore_rules.txt` (replace with production-grade rules)
- Add rule unit tests in Firebase Emulator test suite (new folder)

### Validation
1. Emulator tests for allow/deny matrix by role:
   - non-member
   - member
   - creator
   - payer
   - debtor
2. Negative tests proving blocked cross-group and forged sender writes.

### Exit Criteria
- 100% pass on rules test matrix.
- No open path for unauthorised mutation.

## Phase 2: Trusted Backend for Sensitive Mutations (3-5 days)
### Tasks
1. Implement Cloud Functions (Callable/HTTPS) for:
   - `sendNudge(groupId, expenseId, debtorUid)`
   - `confirmSettlement(expenseId, debtorUid)`
   - `updateGroupStatus(groupId, status)`
2. In each function:
   - derive actor from Auth context
   - verify actor membership and role from Firestore
   - verify group/expense relationship integrity
   - enforce idempotency key where needed
   - write notification and settlement records server-side
3. Block direct client writes in Firestore Rules for these resources/fields.

### Android Refactor Targets
- `app/src/main/java/com/example/fairshare/GroupRepository.java`
- `app/src/main/java/com/example/fairshare/ui/groups/GroupLobbyActivity.java`
- `app/src/main/java/com/example/fairshare/ui/groups/GroupDetailFragment.java`

### Validation
1. Integration tests (Emulator): app client call -> function -> Firestore write.
2. Verify forged senderUid from client is ignored/rejected.
3. Verify unauthorized member receives permission/function error.

### Exit Criteria
- All sensitive transitions occur only through trusted backend endpoints.

## Phase 3: Auditability and Non-Repudiation (2-3 days)
### Tasks
1. Add append-only `audit_events` collection written only by backend.
2. Event model fields:
   - `eventType`
   - `actorUid`
   - `subjectUid`
   - `groupId`
   - `expenseId`
   - `amount`
   - `requestId` (idempotency)
   - `createdAt` (server timestamp)
3. Include events for:
   - nudge sent
   - settlement confirmed
   - group status changed
4. Add admin-only query path for dispute investigation.

### Validation
1. Replay recent test flows and verify complete event chain exists.
2. Attempt client write to `audit_events` and confirm deny.

### Exit Criteria
- Every financial action has immutable server-side evidence.

## Phase 4: Anti-Abuse and DoS Controls (2 days)
### Tasks
1. Add per-user and per-group rate limiting for nudge endpoint.
2. Add cooldown window per debtor/expense pair.
3. Remove/disable client-wide cleanup jobs that touch shared notification dataset.
4. Add abuse telemetry alerts:
   - nudge burst threshold
   - function failure spike

### Android Refactor Targets
- `app/src/main/java/com/example/fairshare/utils/NotificationCleanup.java` (deprecate destructive startup cleanup)
- `app/src/main/java/com/example/fairshare/MainActivity.java` (remove startup cleanup trigger)

### Validation
1. Load-test nudge endpoint and verify throttling behavior.
2. Confirm app gracefully handles throttled responses.

### Exit Criteria
- Spam and quota abuse constrained by policy and observed in telemetry.

## Phase 5: Client Hardening and Entry-Point Security (2 days)
### Tasks
1. Harden exported deep-link activity with mandatory auth + membership re-check before rendering group data.
2. Validate all Intent extras server-side/by fetched records (never trust passed group identifiers).
3. Keep UI role checks, but treat them as presentation only.

### Android Refactor Targets
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/fairshare/ui/groups/GroupLobbyActivity.java`

### Validation
1. Attempt deep-link into non-member group and verify access denied.
2. Attempt tampered extras and verify data not rendered.

### Exit Criteria
- No externally reachable entry path bypasses membership checks.

## Phase 6: Verification, Rollout, and Operations (2-3 days)
### Tasks
1. Create security regression suite in CI:
   - Firestore rule tests
   - function authorization tests
   - critical UI integration tests
2. Staged rollout:
   - canary users
   - 25% rollout
   - 100% rollout
3. Define incident playbooks:
   - forged notification attempt
   - suspicious settlement edits
   - rule misconfiguration rollback

### Exit Criteria
- Security tests mandatory in CI.
- Rollout complete with no critical authz regression.

## 7. Concrete Rule and Function Acceptance Criteria
1. A non-member cannot read any group balance, members, or expenses.
2. A member cannot promote self to creator or change creator-owned state.
3. A debtor cannot mark own payment as confirmed unless policy explicitly allows it.
4. A user cannot forge notification sender identity.
5. A user can only mark own notifications as read.
6. Each settlement and nudge produces one immutable audit event.

## 8. Work Breakdown and Ownership
1. Mobile Engineer:
   - Client refactor to call backend functions
   - Deep-link hardening
   - Error and retry UX
2. Backend/Firebase Engineer:
   - Cloud Functions and idempotency
   - Firestore rules and emulator tests
3. Security Reviewer:
   - Threat-model revalidation
   - Test matrix sign-off
4. QA Engineer:
   - Abuse, authorization, and regression test execution

## 9. Accelerated Delivery Timeline (Plan A Only)

### No-Blaze Fallback Constraint
1. If Firebase Functions cannot be deployed, keep the hardened rules and client-side guards, but disable automated nudge and settlement confirmation flows.
2. Do not re-enable direct client notification writes as a substitute.
3. Treat nudge and settlement automation as pending backend availability.

### Plan A: Finish by Tomorrow (Aggressive, parallel execution)
1. Today (Day 0) - Execution Blocks
    - Block 1 (Hour 0-1): Kickoff + scope lock
       - Owner: Security + Backend + Mobile
       - Output: final schema contract, rules invariants, function contracts, test matrix baseline.
    - Block 2 (Hour 1-3): Firestore Rules hardening + Emulator allow/deny tests
       - Owner: Backend/Firebase
       - Output: hardened rules draft and passing critical negative tests (non-member read deny, forged sender deny, unauthorized settlement deny).
    - Block 3 (Hour 1-3, parallel): Android critical risk removal
       - Owner: Mobile
       - Output: startup notification cleanup disabled and deep-link auth+membership re-check enforced.
    - Block 4 (Hour 3-5): Cloud Functions implementation (v1)
       - Owner: Backend/Firebase
       - Output: callable `sendNudge` and `confirmSettlement` with auth-context actor derivation and membership/role validation.
    - Block 5 (Hour 4-6, parallel): Client integration to backend
       - Owner: Mobile
       - Output: direct sensitive writes replaced with backend calls for nudge and settlement confirmation paths.
    - Day 0 Gate (end of day)
       - Required: rules tests pass, functions deploy to test env, app calls function endpoints successfully in smoke test.

2. Tomorrow (Day 1) - Execution Blocks
    - Block 1 (Hour 0-2): Audit trail + idempotency hardening
       - Owner: Backend/Firebase
       - Output: append-only `audit_events` writes for nudge and settlement confirmation with requestId and server timestamp.
    - Block 2 (Hour 1-3, parallel): Regression + abuse test pass
       - Owner: QA/Security
       - Output: focused pass on spoofing, tampering, privilege escalation, deep-link bypass, and rate abuse behavior.
    - Block 3 (Hour 3-4): Fix-forward window
       - Owner: Mobile + Backend
       - Output: close all critical/high findings from Block 2.
    - Block 4 (Hour 4-5): Canary rollout
       - Owner: Release + QA
       - Output: canary rollout with telemetry watch on permission-denied, function failures, and nudge anomaly thresholds.
    - Block 5 (Hour 5-6): Go/No-Go review
       - Owner: Security + Product + Engineering leads
       - Output: release decision against Tomorrow Minimum Go-Live Criteria.

3. Escalation Rule (for schedule protection)
    - If any critical criterion fails by Day 1 Block 3, freeze feature expansion and spend remaining time only on fixing authz/integrity/privacy blockers.

### Parallelization Model (Required to hit tomorrow target)
1. Backend/Firebase Engineer
   - Firestore Rules + Emulator tests
   - Cloud Functions for trusted writes
2. Mobile Engineer
   - Refactor calls to backend functions
   - Remove risky client cleanup behavior
   - Deep-link auth + membership gate
3. QA/Security
   - Authorization negative tests
   - Abuse and spoofing test pass
   - Go/No-Go checklist sign-off

### Tomorrow Minimum Go-Live Criteria
1. No direct client path can forge sender identity for notifications.
2. No direct client path can mark settlement outside authorized payer flow.
3. Non-members cannot read group financial data in Emulator rule tests.
4. Exported deep-link path blocks unauthorized group access.
5. `audit_events` captures nudge and settlement confirmation actions.

## 10. Immediate Next Actions (Start Today)
1. Implement and test corrected Firestore rules in Emulator first.
2. Build `sendNudge` and `confirmSettlement` Cloud Functions and switch Android calls to them.
3. Disable startup notification cleanup in client.
4. Add auth+membership gate in Group deep-link entry flow.
5. If Blaze is not available, keep the above backend actions blocked in the UI and surface a clear unavailable message instead of attempting a client-side substitute.
