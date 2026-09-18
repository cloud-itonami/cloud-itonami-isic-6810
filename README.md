# cloud-itonami-6810

Open Business Blueprint for **ISIC Rev.5 6810**: real-estate agency
activities (own/leased property — listings, closings, lease management).
This repository publishes a real-estate closing/title-transfer execution
actor as an OSS business that any qualified, licensed operator can fork,
deploy, run, improve and sell.

Built on this workspace's
[`langgraph-clj`](https://github.com/com-junkawasaki/langgraph-clj)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
[`cloud-itonami-M6910`](https://github.com/cloud-itonami/cloud-itonami-M6910)
(Registrar-LLM ⊣ RegistrarGovernor), [`cloud-itonami-6310`](https://github.com/cloud-itonami/cloud-itonami-6310)
(HR-LLM ⊣ PolicyGovernor) and `ai-gftd-itonami` (ops-LLM ⊣ CertGovernor).
Here it is **Realtor-LLM ⊣ RealtorGovernor**.

> **Why an actor layer at all?** An LLM is great at drafting a disclosure
> checklist, normalizing listing intake and flagging a thin KYC file --
> but it has **no notion of which land-registry source is official, no
> legal standing, and no business being the one that decides a real
> title-transfer recording or escrow-fund disbursement happens today**.
> Letting it record a closing or move escrow funds directly invites
> fabricated disclosure requirements, laundering sanctioned parties
> through a purchase, and silent liability for whoever runs it. This
> project seals the Realtor-LLM into a single node and wraps it with an
> independent **RealtorGovernor**, a human **approval workflow**, and an
> immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor drafts and governs a property-closing workflow: listing
intake, per-jurisdiction disclosure/title-document checklisting, buyer/
seller/tenant KYC-sanctions screening, and a closing proposal. It does
**not**, by itself, hold a real-estate broker/agent license in any
jurisdiction, and it does not claim to. Whoever deploys and operates a
live instance (a licensed real-estate agent, a title/escrow company's ops
team, a property-management firm) supplies the jurisdiction-specific
license, the real KYC/AML program and the real land-registry / escrow
integrations, and bears that jurisdiction's liability -- the software
supplies the governed, spec-cited, audited execution scaffold so that
operator does not have to build the compliance layer from scratch for
every new market. This mirrors why `cloud-itonami-6310` is "an OSS
replacement for a HR SaaS you run yourself": nobody centralizes the
liability, many operators can each run their own governed instance.

### Actuation

**A real title-transfer recording or a real escrow-fund disbursement is
never autonomous, at any phase, by construction.** Two independent layers
enforce this (`realty.governor`'s `:actuation` high-stakes gate and
`realty.phase`'s phase table, which never puts `:closing/submit` in any
phase's `:auto` set) -- see `realty.phase`'s docstring and
`test/realty/phase_test.cljk`'s `closing-submit-never-auto-at-any-phase`.
The actor may draft, check, screen and recommend; a human operator (the
licensed agent / closing attorney) is always the one who actually records
the transfer and releases escrow funds.

## The core contract

```
buyer/seller/tenant intake + jurisdiction facts (realty.facts, spec-cited)
        |
        v
   ┌──────────────┐   proposal      ┌───────────────────┐
   │ Realtor-LLM  │ ─────────────▶ │ RealtorGovernor    │  (independent system)
   │   (sealed)    │  + citations   │ spec-basis · KYC   │
   └──────────────┘                 └─────────┬──────────┘
                             commit ◀──────────┼──────────▶ hold (fabricated law;
                                 │                  │         sanctions hit;
                           record + ledger    escalate ─▶ 人間承認    incomplete disclosure;
                                                (ALWAYS for :closing/submit)  un-overridable)
```

**The Realtor-LLM never closes or disburses a record the RealtorGovernor
would reject, and never closes without a human sign-off.** Hard
violations (fabricated jurisdiction disclosure requirements / sanctions
hit / incomplete documents) force **hold** and *cannot* be approved past;
a clean closing proposal still always routes to a human.

## Run

```bash
kbb -M:dev:run     # walk one clean closing + one HARD-hold case through the actor
kbb -M:dev:test    # governor contract · phase invariants · store parity · registry conformance
kbb -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a showing, cleaning and
inspection robot performs the physical property work under the actor,
gated by the independent **RealtorGovernor**. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions require
human sign-off.

## Open business

This repository is not only source code. It is a public, forkable business
model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, RealtorGovernor, closing draft record, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an open
business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

A live sample of the (read-only) operator console is rendered in
[docs/samples/operator-console.html](docs/samples/operator-console.html)
-- pure-data HTML output of `kotoba.property.ui`.

## Capability layer

Resolves via [`kotoba-lang/industry`](https://github.com/kotoba-lang/industry)
(ISIC `6810`). Implemented by:

- [`kotoba-lang/property`](https://github.com/kotoba-lang/property) — parcel, listing, lease, term-overlap

## Layout

| File | Role |
|---|---|
| `src/realty/store.cljk` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + closing-record history |
| `src/realty/registry.cljk` | Closing/title-transfer draft records (no fabricated international check-digit standard -- see docstring) |
| `src/realty/facts.cljk` | Per-jurisdiction disclosure/title requirement catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/realty/realtorllm.cljk` | **Realtor-LLM Advisor** -- `mock-advisor` ‖ `llm-advisor`; intake/disclosure/KYC/closing proposals |
| `src/realty/governor.cljk` | **RealtorGovernor** -- spec-basis · sanctions hold · document-complete · confidence floor · actuation gate |
| `src/realty/phase.cljk` | **Phase 0→3** -- read-only → assisted intake → assisted assess/screen → supervised (closing always human) |
| `src/realty/operation.cljk` | **OperationActor** -- langgraph-clj StateGraph |
| `src/realty/corporate_intel.cljk` | optional cross-reference into [`cloud-itonami-isic-8291`](https://github.com/cloud-itonami/cloud-itonami-isic-8291)'s `:disclosure/screen-name` -- catches a party clean on every LOCAL field but flagged in 8291's own sourced PEP/sanctions data; wired into `screen-kyc` via an injected fn, default is a no-op so every prior caller's behavior is unchanged unless explicitly opted in |
| `src/realty/sim.cljk` | demo driver |
| `src/realty/observation.cljk` | **Observation contract** (`closing-observation/2`) -- provenance-preserving observations of RECORDED registry events over official sources; separate from the actor's own drafts |
| `test/realty/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage · corporate-intelligence integration · observation contract |

## Jurisdiction coverage (honest)

`realty.facts/coverage` reports how many requested jurisdictions actually
have an official spec-basis in `realty.facts/catalog` -- currently 6
seeded (JPN, USA-CA, GBR, DEU, NLD, AUS-NSW) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `realty.facts/catalog`, citing a real official
source -- never fabricate a jurisdiction's requirements to make coverage
look bigger.

### Netherlands closing profile

`NLD` is an implemented jurisdiction profile for owner-operated sale
preparation. It covers the Kadaster title/mortgage/attachment search,
written purchase agreement and consumer cooling-off evidence, notarial
deed draft, Wwft/UBO evidence, mortgage discharge, energy label, and
conditional VvE, erfpacht and tenancy packages. A conditional item needs
the real document or a human-verified not-applicable declaration; the LLM
may not silently omit it.

The broker is optional, but this actor never substitutes for the Dutch
`notaris`: deed approval/signing, Kadaster submission/registration and
release of sale proceeds remain explicit human gates. See
[`docs/nld-operator-guide.md`](docs/nld-operator-guide.md) and ADR-0002.

## The observation contract (`closing-observation/2`)

`realty.observation` is the actor's **observation layer**: how a reading of
an OFFICIAL source (land registry, cadastre, statistics agency) becomes a
provenance-preserving, re-observable claim about **recorded** property
events -- and what such a claim can never become. It is deliberately
SEPARATE from `realty.registry` (the actor's own closing drafts): drafts are
what this actor prepares under a human gate; observations are what external
official sources show. An observation is never an execution and never feeds
back into a closing decision.

One contract, twelve parts: **source receipts** (frozen, content-hash
addressed, id derived from hash + observed-at; an edited receipt is refused,
not re-branded) · **typed subject + events** (a property identified ONLY by
a jurisdiction-scoped registry identifier -- never a street address; typed
registry acts only, so a listing is not an observable event here; party data
and addresses are refused BY CONSTRUCTION) · **measurement window** (every
observation states `{:from :to}`, events must fall inside) · **currency and
area basis** (amounts carry ISO-4217 currency + their own nominal date,
dimensions carry a closed-vocabulary unit, both carry the verbatim raw
transcription; nothing is normalized, converted or combined) · **method /
version** (`closing-observation/2` on every artifact; no model anywhere) ·
**missingness / coverage** (closed flag vocabulary; a jurisdiction without a
`realty.facts` spec-basis must carry `:jurisdiction-spec-basis-absent`, a
recorded transfer without a price figure must carry `:price-unavailable` --
silence would claim completeness) · **derived observations** (`window-observation`
and `coverage-observation`: COUNTS and verbatim registration references
only -- never a price, trend, valuation or market measure) · **refresh
history** (append-only lineage via `:obs/refresh-of`, cross-subject links
refused at append time; `refresh-delta` carries added / removed / changed
figures IN FULL on both sides plus gap movement and both generations'
receipt ids, and computes no numeric difference anywhere) · **Hyakka
proposal** (`hyakka-proposal` builds the claim SHAPE for the `fudosan`
corpus -- receipts, verbatim values, bases, gaps, the scope's epistemic and
privacy boundaries, `:no-model true`; prop names are contract-local and
flagged unregistered; this contract transmits nothing anywhere) · **query /
readback** (`readback` revalidates everything it returns and refuses
tampered receipts; a miss is a miss, never a default; `readback-chain`
walks the full lineage oldest-first, refusing truncated, cyclic or
cross-subject chains) · **history discipline** (duplicate ids refused; a
parcel is not a building -- re-typing a subject is refused) · **refusals**
(53 loud `:refusal/code` failures instead of quiet degradation).

WHAT THIS CONTRACT NEVER PRODUCES: a valuation, a market score, a ranking
of properties / neighbourhoods / jurisdictions, an ownership claim about any
person (a registered title is not beneficial ownership, and parties are not
carried at all), or investment advice of any kind. Recorded-transaction
prices are observations of what a source disclosed -- not current market
value, not a valuation service, and not an offer.

Deterministic contract tests: `test/realty/observation_test.cljk` -- 32
tests over synthetic fixtures only (marked as such; the receipt URLs are
the catalog's own provenance citations; no network, no I/O, no model).

## License

Code and implementation templates are AGPL-3.0-or-later.
