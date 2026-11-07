# ADR-0003: Japan actuation authority and human gates (provisional); already-closed check

- Status: accepted (JPN gates provisional)
- Date: 2026-11-07
- Tracking: Linear JUN-980, under JUN-976 / JUN-990

## Context

`realty.facts` carried `:actuation-authority` and `:human-gates` only for NLD
(ADR-0002). JPN had an owner authority and a document list but did not say who
acts. Separately, `:closing/submit` had no check against a listing that is
already closed: a second closing proposal escalated to a human approver
instead of holding.

## Decision

1. Fill JPN `:actuation-authority` and `:human-gates`: the parties or a
   judicial scrivener (司法書士) file the registration application, the
   registrar (登記官) of the 法務局 completes it, and the actor never files,
   signs or releases funds. Gates: important-matters explanation (by a licensed
   宅地建物取引士), contract signed, identity confirmed, registration tax paid,
   application filed, registration completed, funds released.
2. Mark the JPN profile `:gates-status :provisional`. It is a first modelling
   from the structure of 不動産登記法 and has not been reviewed by a licensed
   professional in Japan. Per the jurisdiction-pack policy (JUN-984) a pack is
   final only once a local qualified person has checked and signed it. The gate
   set and order may change at that review.
3. Add `:already-closed` as a fourth HARD governor check for `:closing/submit`.
   A listing is closed when its `:status` is `:closed` or it has a
   `:closing-number`. A human approver cannot override it.

## Consequences

- No behaviour that previously committed now holds, except a second closing
  proposal for an already-closed listing (the intended change).
- The gates are data only. The governor does not yet enforce gate completion;
  enforcement is tracked with the credential and `:requires` work (JUN-979).
- Open: statutory basis for each gate, and whether the set is complete, are
  unverified.
