# Operator Quickstart

Get the Community Real-Estate Agency running locally in under 5 minutes.

## Prerequisites

- **Clojure CLI** (≥1.11) — [install](https://clojure.org/guides/install_clojure)
- **Git** — clone this repository
- **langgraph-clj** — included as `:local/root` dependency in `deps.edn` (workspace checkout only; standalone forks should use git coordinates)

If you're working in the monorepo, all dependencies resolve to local checkouts:
- `io.github.com-junkawasaki/langgraph-clj` → `../../kotoba-lang/langgraph`
- `io.github.cloud-itonami/cloud-itonami-isic-8291` → `../cloud-itonami-isic-8291` (optional corporate intelligence integration)

## Run Tests

```bash
clojure -M:dev:test
```

This runs the full test suite:
- **Governor contract** — spec-basis citations, KYC hold enforcement, actuation gate (never auto-closes)
- **Phase invariants** — read-only → assisted intake → assess/screen → supervised (closing always human)
- **Store parity** — in-memory ‖ Datomic consistency
- **Registry conformance** — closing record spec-compliance
- **Facts coverage** — honest reporting of jurisdiction coverage (currently 6 seeded; no fabrication)

Key invariant verified: **`:closing/submit` is never in any phase's `:auto` set.** A human is always the one who records the title transfer and releases escrow.

## Run the Demo

```bash
clojure -M:dev:run
```

This walks two scenarios through the actor:
1. A clean closing proposal (passes all governors, routes to human approval)
2. A hard-hold case (sanctions hit, un-overridable, escalates)

Output includes the closing record, audit ledger entries, and governor decision logs.

## Understand the Governor

The **RealtorGovernor** is the independent approval layer that wraps the Realtor-LLM.

- **File:** `src/realty/governor.cljc`
- **Role:** spec-basis validation, sanctions screening, document completeness, confidence floor, actuation gate
- **Key gate:** `(governor/actuation governor ...)` — controls whether a high-stakes action (title recording, escrow disbursement) can proceed (returns `:hold` or `:commit`)
- **Spec basis:** All holds cite the jurisdiction's official requirement source; no fabricated law
- **Contract:** Once a hold is issued (e.g., sanctions hit), it **cannot be overridden** by any phase or role

Related files:
- `src/realty/phase.cljc` — phase-table enforcement (0→3, closing always supervised)
- `src/realty/operation.cljc` — the StateGraph actor that orchestrates Realtor-LLM → Governor → human approval
- `src/realty/facts.cljc` — per-jurisdiction disclosure/title requirement catalog with spec-basis citations
- `test/realty/phase_test.clj` — test verifying `:closing/submit` is never auto-approved at any phase

## Access the Operator Console

A read-only sample of the operator console (pure-data HTML output) is rendered at:

```
docs/samples/operator-console.html
```

This shows how the UI renders listings, closings, audit trails, and governor holds.

## Deploy to Production

See [`docs/operator-guide.md`](operator-guide.md) for:
- First deployment checklist (register agency, import listings, configure escalation)
- Minimum production controls (parcel ID validation, term-overlap gates, audit exports)
- Actuation gates (no recording, no escrow release without human sign-off)

For jurisdiction-specific details (e.g., Dutch closing profile), see [`docs/nld-operator-guide.md`](nld-operator-guide.md) and the architecture decisions in [`docs/adr/`](adr/).

## Next Steps

1. **Fork** this repo on GitHub
2. **Customize** `src/realty/facts.cljc` with your jurisdiction's requirements
3. **Extend** `src/realty/corporate_intel.cljc` to integrate your own sanctions/PEP data sources
4. **Deploy** with your real KYC/AML program, land-registry integration, and escrow partner
5. **Operate** as a licensed agent/operator; the actor provides the governance scaffold, not the license

## Support

- **Architecture & decisions:** [`docs/adr/`](adr/)
- **Full README:** [`../README.md`](../README.md)
- **Business model:** [`business-model.md`](business-model.md)
- **License:** AGPL-3.0-or-later — see [`../LICENSE`](../LICENSE)
