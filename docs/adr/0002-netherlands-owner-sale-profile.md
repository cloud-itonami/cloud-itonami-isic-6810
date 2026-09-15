# ADR-0002: Netherlands owner-sale closing profile

- Status: accepted
- Date: 2026-07-11
- Last updated: 2026-07-12
- Implemented by: `6a4d291` (`feat: add Netherlands property sale profile`)

## Context

The 6810 actor already governed listing intake, jurisdiction assessment,
party screening and human-approved closing drafts, but NLD had no official
spec-basis. The separate `cloud-itonami-iso3166-nld` actor covers market
entry/procurement and cannot by itself establish Dutch real-property title.

## Decision

Add NLD to `realty.facts` as the transaction-law profile and keep the
country actor as an optional source for operator/business identity. The
NLD checklist cites Dutch legislation, Kadaster, RVO and the notarial AML
framework. Conditional VvE, erfpacht, mortgage and tenancy branches require
evidence or an explicit human-verified not-applicable declaration.

Model the Dutch notary and Kadaster boundary as data (`:actuation-authority`
and `:human-gates`) in addition to the existing structural invariant that
`:closing/submit` never auto-commits. The actor prepares a draft package;
it never executes the deed, registers title or releases funds.

Store transaction documents outside Git in encrypted storage. A case file
contains document identifiers, hashes, provenance, retrieval timestamps and
expiry/re-verification dates, but not passports, deeds, UBO records or other
personal documents themselves.

## Implemented contract

The implementation adds `NLD` to `realty.facts/catalog` with:

- Kadaster as owner/registry authority;
- BW Book 3 article 89, BW Book 7 article 2 and Wwft as the core legal basis;
- official source links for transfer law, residential sale, registration,
  energy labels and notarial AML;
- a 12-item evidence checklist covering title, mortgages/attachments,
  purchase agreement, notarial deed draft, identity/UBO/Wwft, discharge,
  EP-online label, VvE, erfpacht, occupancy/tenancy, municipal/defect
  disclosures and closing funds instructions;
- explicit human gates for notary approval, deed signing, Kadaster
  registration and notary-controlled release of funds.

Conditional evidence is not optional-by-default. VvE, erfpacht, mortgage,
tenancy and energy-label branches require the applicable evidence or a
human-verified not-applicable/exemption declaration.

## Case initiation

A live NLD sale starts only after the operator records the property address,
Kadaster parcel reference if known, every owner, ownership vehicle, property
and occupancy classification, VvE/erfpacht/mortgage status, desired timing,
operator residency and existing broker/notary appointments.

The case then progresses through:

1. Kadaster title, parcel, mortgage, attachment and limited-right retrieval;
2. property classification and conditional-evidence branching;
3. energy-label, VvE, erfpacht, tenancy, permit and defect evidence assembly;
4. broker choice (optional), valuation/listing and offer governance;
5. notary selection and independent Wwft review;
6. written agreement and cooling-off evidence where applicable;
7. notary closing package and human-approved `:closing/submit`;
8. external deed execution, Kadaster registration and notary funds release.

The executable operational detail is maintained in
[`../nld-operator-guide.md`](../nld-operator-guide.md).

## Verification

At implementation, the complete suite passed with 31 tests and 128
assertions, with zero failures/errors. `clj-kondo` completed with zero
errors/warnings. Tests pin NLD's official authority, legal basis, notarial
actuation authority, four human gates, conditional evidence categories and
full-checklist completeness behavior. Existing phase tests independently
pin that `:closing/submit` is never auto-eligible.

## Consequences

- NLD assessment and closing drafts can use a sourced checklist rather
  than being held as an unknown jurisdiction.
- A broker remains optional; a Netherlands civil-law notary does not.
- Municipality-, property- and tax-specific decisions remain outside this
  generic profile and require current human professional review.
- Future connectors return evidence only; they must not weaken the governor
  or translate an external API success into autonomous actuation.
- The first live case requires a private case-storage implementation and
  current property-specific review; this ADR does not authorize storing
  personal transaction documents in the repository.
